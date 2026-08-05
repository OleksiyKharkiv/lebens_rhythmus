package com.be.service;

import com.be.domain.entity.Group;
import com.be.domain.entity.User;
import com.be.domain.entity.enums.EnrollmentStatus;
import com.be.domain.entity.enums.Role;
import com.be.domain.repository.EnrollmentRepository;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.UserRepository;
import com.be.web.dto.response.AdminMetricsDTO;
import com.be.web.dto.response.GroupFillRateDTO;
import com.be.web.dto.response.RegistrationTrendPointDTO;
import com.be.web.dto.response.RetentionDTO;
import com.be.web.dto.response.WorkshopAlertDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// LR-015 admin/owner dashboard metrics (Круглый стол #2/#3, 2026-08-05).
// M2/M3 deliberately not implemented here — blocked on the registration/
// payment confirmation mechanism, see LR-017.
@Service
@RequiredArgsConstructor
public class MetricsService {

    private static final int TREND_DAYS = 30;

    // "Occupies a spot" for fill-rate purposes = PENDING (awaiting payment/
    // confirmation) or CONFIRMED. CANCELLED does not — confirmed with the
    // customer 2026-08-05 (diverges on purpose from Group.getEnrolledCount(),
    // which still counts every status; that's a pre-existing, separately
    // ticketed gap on the public workshop page, not fixed here).
    private static final Set<EnrollmentStatus> COUNTS_TOWARD_FILL =
            EnumSet.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public AdminMetricsDTO getMetrics() {
        List<GroupFillRateDTO> fillRates = computeFillRates();
        return AdminMetricsDTO.builder()
                .fillRates(fillRates)
                .registrationTrend(computeRegistrationTrend())
                .attentionAlerts(computeAttentionAlerts(fillRates))
                .retention(computeRetention())
                .build();
    }

    private List<GroupFillRateDTO> computeFillRates() {
        return groupRepository.findByActiveTrue().stream()
                .map(this::toFillRate)
                .sorted(Comparator.comparing(GroupFillRateDTO::getStartDateTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private GroupFillRateDTO toFillRate(Group g) {
        long counted = g.getEnrollments() == null ? 0 :
                g.getEnrollments().stream()
                        .filter(e -> COUNTS_TOWARD_FILL.contains(e.getStatus()))
                        .count();
        double ratio = g.getCapacity() == 0 ? 0.0 : (double) counted / g.getCapacity();
        return GroupFillRateDTO.builder()
                .groupId(g.getId())
                .workshopTitle(g.getWorkshop() != null ? g.getWorkshop().getWorkshopName() : null)
                .groupTitle(g.getTitleDe())
                .startDateTime(g.getStartDateTime())
                .capacity(g.getCapacity())
                .enrolledCount((int) counted)
                .fillRatio(ratio)
                .build();
    }

    // M5 thresholds — Круглый стол #3 draft, confirmed by заказчик 2026-08-05:
    // 7d/<30% info, 5d/<50% warning, 3d/<70% urgent, 1d/<90% critical.
    // Checked tightest-deadline-first so a group failing several thresholds
    // at once gets its most urgent applicable level.
    private List<WorkshopAlertDTO> computeAttentionAlerts(List<GroupFillRateDTO> fillRates) {
        LocalDateTime now = LocalDateTime.now();
        List<WorkshopAlertDTO> alerts = new ArrayList<>();
        for (GroupFillRateDTO g : fillRates) {
            if (g.getStartDateTime() == null || !g.getStartDateTime().isAfter(now)) continue;
            long daysUntilStart = ChronoUnit.DAYS.between(now.toLocalDate(), g.getStartDateTime().toLocalDate());
            String level = alertLevel(daysUntilStart, g.getFillRatio());
            if (level == null) continue;
            alerts.add(WorkshopAlertDTO.builder()
                    .groupId(g.getGroupId())
                    .workshopTitle(g.getWorkshopTitle())
                    .groupTitle(g.getGroupTitle())
                    .startDateTime(g.getStartDateTime())
                    .daysUntilStart(daysUntilStart)
                    .fillRatio(g.getFillRatio())
                    .level(level)
                    .build());
        }
        return alerts;
    }

    // Package-private (not private) so MetricsServiceTest can exercise the
    // threshold boundaries directly, without a full Spring context.
    String alertLevel(long daysUntilStart, double fillRatio) {
        if (daysUntilStart <= 1 && fillRatio < 0.90) return "critical";
        if (daysUntilStart <= 3 && fillRatio < 0.70) return "urgent";
        if (daysUntilStart <= 5 && fillRatio < 0.50) return "warning";
        if (daysUntilStart <= 7 && fillRatio < 0.30) return "info";
        return null;
    }

    private List<RegistrationTrendPointDTO> computeRegistrationTrend() {
        LocalDate startDay = LocalDate.now().minusDays(TREND_DAYS - 1L);
        List<User> newUsers = userRepository.findByRoleAndCreatedAtAfter(Role.USER, startDay.atStartOfDay());
        Map<LocalDate, Long> byDay = newUsers.stream()
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<RegistrationTrendPointDTO> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (LocalDate day = startDay; !day.isAfter(today); day = day.plusDays(1)) {
            trend.add(RegistrationTrendPointDTO.builder()
                    .date(day)
                    .newUsers(byDay.getOrDefault(day, 0L))
                    .build());
        }
        return trend;
    }

    private RetentionDTO computeRetention() {
        List<Object[]> rows = enrollmentRepository.countDistinctWorkshopsPerUserWithStatus(
                EnrollmentStatus.CONFIRMED, Role.USER);
        long total = rows.size();
        long repeat = rows.stream().filter(r -> ((Number) r[1]).longValue() >= 2).count();
        double rate = total == 0 ? 0.0 : (double) repeat / total;
        return RetentionDTO.builder()
                .totalCustomers(total)
                .repeatCustomers(repeat)
                .retentionRate(rate)
                .build();
    }
}
