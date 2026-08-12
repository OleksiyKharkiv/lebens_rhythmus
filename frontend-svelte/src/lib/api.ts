// Thin client for the Spring Boot backend (separate app, see
// backend/src/main/java/com/be/web/controller/AuthController.java).
// Mirrors the contract the old static frontend used against the same API.

export const API_BASE_URL = (() => {
	if (typeof window === 'undefined') return 'http://localhost:8080/api/v1';
	const host = window.location.hostname;
	return host === 'localhost' || host === '127.0.0.1'
		? 'http://localhost:8080/api/v1'
		: 'https://api.tlab29.com/api/v1';
})();

export interface LoginResponse {
	token: string;
	tokenType: string;
	expiresIn: number;
	id: number;
	email: string;
	firstName: string;
	lastName: string;
	role: string;
}

export class ApiError extends Error {
	status: number;
	// Stable machine-readable discriminator from GlobalExceptionHandler.java
	// (e.g. "EMAIL_NOT_VERIFIED") — for anything the UI needs to branch on,
	// don't parse the human message string, that's for display only.
	code?: string;
	constructor(message: string, status: number, code?: string) {
		super(message);
		this.status = status;
		this.code = code;
	}
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
	const res = await fetch(`${API_BASE_URL}${path}`, {
		...options,
		headers: { 'Content-Type': 'application/json', ...(options.headers ?? {}) }
	});
	const text = await res.text();
	const data = text ? JSON.parse(text) : null;
	if (!res.ok) {
		throw new ApiError(data?.message ?? data?.error ?? `HTTP ${res.status}`, res.status, data?.code);
	}
	return data as T;
}

export function login(email: string, password: string) {
	return request<LoginResponse>('/auth/login', {
		method: 'POST',
		body: JSON.stringify({ email, password })
	});
}

export interface RegistrationResponse {
	message: string;
	email: string;
}

// Registering no longer logs the user in (LR: email verification is now
// mandatory before login) — the response is just a "check your email"
// confirmation, not a session.
// Found live in prod 2026-08-12: acceptedTerms/privacyPolicyAccepted were
// checked client-side (login/+page.svelte's handleRegister) but never
// actually sent — UserRegistrationDTO.{acceptedTerms,privacyPolicyAccepted}
// are @AssertTrue with no value from the request body, so they always
// defaulted to false and every single registration 400'd, regardless of
// what the user actually checked.
export function register(input: {
	firstName: string;
	lastName: string;
	email: string;
	password: string;
	acceptedTerms: boolean;
	privacyPolicyAccepted: boolean;
}) {
	return request<RegistrationResponse>('/auth/register', {
		method: 'POST',
		body: JSON.stringify(input)
	});
}

export function verifyEmail(token: string) {
	return request<void>('/auth/verify-email', {
		method: 'POST',
		body: JSON.stringify({ token })
	});
}

// Always resolves regardless of whether the email exists or is already
// verified — the backend deliberately never reveals which, to avoid
// account enumeration (see AuthService.resendVerification).
export function resendVerification(email: string) {
	return request<void>('/auth/resend-verification', {
		method: 'POST',
		body: JSON.stringify({ email })
	});
}

export function persistSession(data: LoginResponse) {
	if (typeof window === 'undefined') return;
	localStorage.setItem('authToken', data.token);
	localStorage.setItem('tokenExpiry', String(Date.now() + data.expiresIn * 1000));
	localStorage.setItem('userData', JSON.stringify({ id: data.id, email: data.email, role: data.role }));
}

export function isAuthenticated(): boolean {
	if (typeof window === 'undefined') return false;
	const token = localStorage.getItem('authToken');
	const expiry = localStorage.getItem('tokenExpiry');
	return !!(token && expiry && Date.now() < Number(expiry));
}

export function clearSession() {
	if (typeof window === 'undefined') return;
	localStorage.removeItem('authToken');
	localStorage.removeItem('tokenExpiry');
	localStorage.removeItem('userData');
}

/**
 * Role cached at login time (persistSession) — lets role-gated layouts
 * (admin/teacher) guard client-side without an extra /users/me round trip.
 * Still only a UX guard, same caveat as authRequest below.
 */
export function getStoredRole(): string | null {
	if (typeof window === 'undefined') return null;
	const raw = localStorage.getItem('userData');
	if (!raw) return null;
	try {
		return JSON.parse(raw).role ?? null;
	} catch {
		return null;
	}
}

/**
 * Pattern for endpoints under `/users/me`, `/workshops/{id}/enroll`, etc.
 * (LR-ADR-016 personal cabinet, LR-ADR-004 teacher role) — none of the 3
 * pages built so far need this, added ahead of Wave 2 per architect-reviewer
 * (decide the Bearer/401 pattern once, not three times per role area).
 * `adapter-static` means there's no server here — this is the client-side
 * UX guard only; the backend's own @PreAuthorize is the real boundary.
 */
async function authRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
	if (typeof window === 'undefined') {
		throw new ApiError('authRequest called outside the browser', 0);
	}
	const token = localStorage.getItem('authToken');
	try {
		return await request<T>(path, {
			...options,
			headers: {
				...(options.headers ?? {}),
				...(token ? { Authorization: `Bearer ${token}` } : {})
			}
		});
	} catch (err) {
		if (err instanceof ApiError && err.status === 401) {
			clearSession();
			window.location.href = '/login';
		}
		throw err;
	}
}

export function getCurrentUser() {
	return authRequest<{
		id: number;
		email: string;
		firstName: string;
		lastName: string;
		role: string;
	}>('/users/me');
}

// ---------- Public catalog (Wave 1 entities) ----------
// Mirrors backend/src/main/java/com/be/web/dto/response/*ResponseDTO.java —
// keep these in sync if the backend DTOs change shape.

export interface ActivityDTO {
	id: number;
	titleDe: string;
	titleEn: string;
	titleUa: string;
	descriptionDe: string;
	descriptionEn: string;
	descriptionUa: string;
	price: number;
	durationMinutes: number;
	active: boolean;
}

export interface WorkshopListItem {
	id: number;
	title: string;
	shortDescription: string;
	teacher: { id: number; firstName: string; lastName: string } | null;
	startDate: string | null;
	endDate: string | null;
	// venueName removed (LR-015) — venue moved to Group/GroupDTO, a
	// workshop's groups can each run at a different place now.
	price: number | null;
	status: string;
}

// Full real shape (backend GroupDTO.java) — activityId/teacherId/ageGroupId/
// languageId added for the admin Groups page (LR-ADR-004), the
// workshop-detail page only ever needed the first-half subset.
export interface GroupDTO {
	id: number;
	titleDe: string;
	titleEn: string;
	titleUa: string;
	startDateTime: string;
	endDateTime: string | null;
	capacity: number;
	enrolledCount: number;
	workshopId: number | null;
	workshopTitle: string | null;
	activityId: number | null;
	teacherId: number | null;
	ageGroupId: number | null;
	// LR-015 — "titleDe (min–max)", composed server-side (GroupMapper).
	ageGroupName: string | null;
	languageId: number | null;
	// LR-015 — venueName already includes the room (backend composes
	// "name — room"), one venues row is one physical room.
	venueId: number | null;
	venueName: string | null;
	active: boolean;
}

export interface WorkshopFileDTO {
	id: number;
	filename: string;
	url: string;
	contentType: string;
	fileSize: number;
}

export interface WorkshopDetail {
	id: number;
	title: string;
	description: string;
	teacher: { id: number; firstName: string; lastName: string } | null;
	startDate: string | null;
	endDate: string | null;
	// venueName/venueId removed (LR-015) — see each group in `groups` below.
	price: number | null;
	status: string;
	groups: GroupDTO[];
	// Not consumed by any page yet — added so its shape is right when
	// workshop media surfaces in the personal dashboard (LR-ADR-016).
	files: WorkshopFileDTO[];
	totalEnrollments: number | null;
}

export interface PerformanceDTO {
	id: number;
	workshopId: number | null;
	workshopTitle: string | null;
	title: string;
	description: string;
	performanceDate: string;
	venue: string | null;
	maxAttendees: number | null;
	status: string;
}

export function getActivities() {
	return request<ActivityDTO[]>('/activities');
}

export function getWorkshops(upcoming = true) {
	return request<WorkshopListItem[]>(`/workshops${upcoming ? '?upcoming=true' : ''}`);
}

export function getWorkshop(id: string | number) {
	return request<WorkshopDetail>(`/workshops/${id}`);
}

export function getPerformances() {
	return request<PerformanceDTO[]>('/performances');
}

export function enrollInWorkshop(workshopId: string | number, groupId?: number) {
	return authRequest<{ status: string }>(`/workshops/${workshopId}/enroll`, {
		method: 'POST',
		body: JSON.stringify(groupId ? { groupId } : {})
	});
}

// Real contract per backend/.../web/dto/request/FeedbackRequestDTO.java:
// just `content` + `rating` — the OLD frontend's feedback.js sent
// feedbackType/subject/message/email, which never matched this DTO at all
// (found while porting this page, not previously documented).
export function submitFeedback(input: { content: string; rating?: number }) {
	return authRequest<{ id: number }>('/feedbacks', {
		method: 'POST',
		body: JSON.stringify(input)
	});
}

// ---------- Personal dashboard (LR-ADR-016: schedule + media + payments) ----------

export interface EnrollmentDTO {
	id: number;
	workshopId: number;
	workshopTitle: string;
	groupId: number | null;
	groupTitle: string | null;
	status: string;
	createdAt: string;
}

export function getMyEnrollments() {
	return authRequest<EnrollmentDTO[]>('/users/me/enrollments');
}

// Mirrors PaymentResponseDTO.java minus `note` — the backend's /payments/me
// (PaymentMapper.toSelfViewDTO, LR-004) never sends it to this endpoint, by
// deliberate decision: it's an admin/accounting reference field, not
// customer-facing (confirmed 2026-07-23, no ERM design ever specced a
// customer-visible payment note).
export interface PaymentDTO {
	id: number;
	orderId: number | null;
	orderNumber: string | null;
	amount: number;
	currency: string;
	provider: string | null;
	methodName: string | null;
	status: string;
	paidAt: string | null;
	createdAt: string;
}

export function getMyPayments() {
	return authRequest<PaymentDTO[]>('/payments/me');
}

// ---------- Admin panel + teacher dashboard (LR-ADR-004) ----------
// Mirrors real backend DTOs (verified against source, not the old static
// site's JS — that JS had several stale/wrong payload shapes, see
// CHANGELOG.md 2026-07-23 for the specifics fixed here).

export type Role = 'ADMIN' | 'BUSINESS_OWNER' | 'USER' | 'TEACHER' | 'CONTENT_MANAGER';

export interface UserBasicDTO {
	id: number;
	email: string;
	firstName: string;
	lastName: string;
	role: Role;
	enabled: boolean;
}

export function getAllUsers() {
	return authRequest<UserBasicDTO[]>('/users');
}

export function searchUsers(query: string) {
	return authRequest<UserBasicDTO[]>(`/users/search?query=${encodeURIComponent(query)}`);
}

export function updateUserRole(userId: number, role: Role) {
	return authRequest<UserBasicDTO>(`/users/${userId}/role?role=${role}`, { method: 'PUT' });
}

export function deactivateUser(userId: number) {
	return authRequest<string>(`/users/${userId}`, { method: 'DELETE' });
}

// Added this session (LR-007) — no counterpart to deactivate existed before.
export function reactivateUser(userId: number) {
	return authRequest<string>(`/users/${userId}/reactivate`, { method: 'PUT' });
}

export interface UserStatistics {
	totalUsers: number;
	activeUsers: number;
	userCount: number;
	teacherCount: number;
	adminCount: number;
}

export function getUserStatistics() {
	return authRequest<UserStatistics>('/users/stats/count');
}

// ----- Admin dashboard metrics (LR-015, M1/M4/M5/M6) -----
// M2/M3 intentionally absent — blocked on the registration/payment
// confirmation mechanism (LR-017), not built yet.

export interface GroupFillRateDTO {
	groupId: number;
	workshopTitle: string | null;
	groupTitle: string;
	startDateTime: string;
	capacity: number;
	enrolledCount: number;
	fillRatio: number;
}

export interface RegistrationTrendPointDTO {
	date: string;
	newUsers: number;
}

export type AlertLevel = 'info' | 'warning' | 'urgent' | 'critical';

export interface WorkshopAlertDTO {
	groupId: number;
	workshopTitle: string | null;
	groupTitle: string;
	startDateTime: string;
	daysUntilStart: number;
	fillRatio: number;
	level: AlertLevel;
}

export interface RetentionDTO {
	totalCustomers: number;
	repeatCustomers: number;
	retentionRate: number;
}

export interface AdminMetricsDTO {
	fillRates: GroupFillRateDTO[];
	registrationTrend: RegistrationTrendPointDTO[];
	attentionAlerts: WorkshopAlertDTO[];
	retention: RetentionDTO;
}

export function getAdminMetrics() {
	return authRequest<AdminMetricsDTO>('/admin/metrics');
}

// ----- Activities -----

export interface ActivityRequestDTO {
	titleDe: string;
	titleEn: string;
	titleUa: string;
	descriptionDe: string;
	descriptionEn: string;
	descriptionUa: string;
	price: number;
	durationMinutes: number;
	active: boolean;
}

export function createActivity(input: ActivityRequestDTO) {
	return authRequest<ActivityDTO>('/activities', { method: 'POST', body: JSON.stringify(input) });
}

export function updateActivity(id: number, input: ActivityRequestDTO) {
	return authRequest<ActivityDTO>(`/activities/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function deleteActivity(id: number) {
	return authRequest<void>(`/activities/${id}`, { method: 'DELETE' });
}

// ----- Venues -----

// Real backend response shape — Java String fields can genuinely be null.
export interface VenueDTO {
	id: number;
	name: string;
	// LR-015 — one physical room = one venues row (two rooms in the same
	// building are two rows sharing name/address, distinguished by this).
	room: string | null;
	address: string;
	city: string;
	postalCode: string | null;
	country: string | null;
	capacity: number | null;
	description: string | null;
	contactPhone: string | null;
	contactEmail: string | null;
}

// Form/request shape — always plain strings ('' instead of null for an
// empty optional field), since the Input component binds a non-nullable
// string. The backend accepts '' the same way it accepts null here.
export interface VenueRequestDTO {
	name: string;
	room: string;
	address: string;
	city: string;
	postalCode: string;
	country: string;
	capacity: number | null;
	description: string;
	contactPhone: string;
	contactEmail: string;
}

// GET /venues has no @PreAuthorize (open to any authenticated user, not
// admin-restricted) but SecurityConfig's permitAll list only covers
// workshops/activities/performances — this still needs a Bearer token,
// found by architect-reviewer (was wrongly using the unauthenticated
// request() here, which would 401 against a real backend).
export function getVenues() {
	return authRequest<VenueDTO[]>('/venues');
}

export function createVenue(input: VenueRequestDTO) {
	return authRequest<VenueDTO>('/venues', { method: 'POST', body: JSON.stringify(input) });
}

export function updateVenue(id: number, input: VenueRequestDTO) {
	return authRequest<VenueDTO>(`/venues/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function deleteVenue(id: number) {
	return authRequest<void>(`/venues/${id}`, { method: 'DELETE' });
}

// ----- Age groups -----

export interface AgeGroupDTO {
	id: number;
	titleDe: string;
	titleEn: string;
	titleUa: string;
	minAge: number;
	maxAge: number;
}

export interface AgeGroupRequestDTO {
	titleDe: string;
	titleEn: string;
	titleUa: string;
	minAge: number;
	maxAge: number;
}

// Same authRequest correction as getVenues() above — GET /age-groups
// requires a valid JWT under the current SecurityConfig (not in the
// permitAll list) even though it has no @PreAuthorize of its own.
export function getAgeGroups() {
	return authRequest<AgeGroupDTO[]>('/age-groups');
}

export function createAgeGroup(input: AgeGroupRequestDTO) {
	return authRequest<AgeGroupDTO>('/age-groups', { method: 'POST', body: JSON.stringify(input) });
}

export function updateAgeGroup(id: number, input: AgeGroupRequestDTO) {
	return authRequest<AgeGroupDTO>(`/age-groups/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function deleteAgeGroup(id: number) {
	return authRequest<void>(`/age-groups/${id}`, { method: 'DELETE' });
}

// ----- Workshops (admin) -----

export type WorkshopStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED' | 'CANCELLED';

export interface WorkshopCreateDTO {
	title: string;
	description: string;
	teacherId: number | null;
	startDate: string | null;
	endDate: string | null;
	// venueId removed (LR-015) — venue is set per-Group now, see GroupWriteDTO.venue.
	maxParticipants: number | null;
	price: number | null;
	status: WorkshopStatus;
}

export function createWorkshop(input: WorkshopCreateDTO) {
	return authRequest<WorkshopDetail>('/workshops', { method: 'POST', body: JSON.stringify(input) });
}

export function updateWorkshop(id: number, input: WorkshopCreateDTO) {
	return authRequest<WorkshopDetail>(`/workshops/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function deleteWorkshop(id: number) {
	return authRequest<void>(`/workshops/${id}`, { method: 'DELETE' });
}

// LR-072 — teacherId here is now Teacher.id, same id space as
// getGroupsByTeacherId below (Workshop.teacher was migrated from User to
// Teacher; previously this took a User.id, a real bug — the backend's
// @PreAuthorize self-check always compares against a resolved Teacher.id,
// so a TEACHER caller passing their own User.id was silently rejected).
export function getWorkshopsByTeacherId(teacherId: number) {
	return authRequest<WorkshopListItem[]>(`/workshops/teacher/${teacherId}`);
}

// ----- Courses (LR-069, LR-ADR-023) -----
// Purely descriptive/marketing entity, no schedule fields — regularity
// lives on Group once LR-081 ships. teacherId here = User.id, same
// temporary shape as Workshop.teacherId above (see LR-072) — use
// searchUsers() above to look one up by lastname/email, not getTeachers().

export interface CourseListItem {
	id: number;
	titleDe: string;
	titleEn: string;
	titleUa: string;
	shortDescriptionDe: string | null;
	teacher: { id: number; firstName: string; lastName: string } | null;
	isOnline: boolean;
	isSynchronous: boolean;
	hasRecordings: boolean;
}

export interface CourseDetail {
	id: number;
	titleDe: string;
	titleEn: string;
	titleUa: string;
	descriptionDe: string | null;
	descriptionEn: string | null;
	descriptionUa: string | null;
	ageGroupId: number | null;
	ageGroupName: string | null;
	teacher: { id: number; firstName: string; lastName: string } | null;
	isOnline: boolean;
	isSynchronous: boolean;
	hasRecordings: boolean;
	formatDisclaimerDe: string | null;
	formatDisclaimerEn: string | null;
	formatDisclaimerUa: string | null;
}

export interface CourseCreateDTO {
	titleDe: string;
	titleEn: string;
	titleUa: string;
	descriptionDe: string;
	descriptionEn: string;
	descriptionUa: string;
	ageGroupId: number | null;
	teacherId: number | null;
	isOnline: boolean;
	isSynchronous: boolean;
	hasRecordings: boolean;
	formatDisclaimerDe: string;
	formatDisclaimerEn: string;
	formatDisclaimerUa: string;
}

export function getCourses() {
	return request<CourseListItem[]>('/courses');
}

export function getCourse(id: string | number) {
	return request<CourseDetail>(`/courses/${id}`);
}

export function createCourse(input: CourseCreateDTO) {
	return authRequest<CourseDetail>('/courses', { method: 'POST', body: JSON.stringify(input) });
}

export function updateCourse(id: number, input: CourseCreateDTO) {
	return authRequest<CourseDetail>(`/courses/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function deleteCourse(id: number) {
	return authRequest<void>(`/courses/${id}`, { method: 'DELETE' });
}

// ----- Teachers (Teacher entity, NOT User) -----

export interface TeacherInfoDTO {
	id: number;
	firstName: string;
	lastName: string;
	email: string;
	phone: string | null;
	title: string | null;
	approved: boolean;
	bioDe: string | null;
	bioEn: string | null;
	bioUa: string | null;
	active: boolean;
}

// Same authRequest correction as getVenues() above — GET /teachers requires
// a valid JWT under the current SecurityConfig even though it has no
// @PreAuthorize of its own.
export function getTeachers() {
	return authRequest<TeacherInfoDTO[]>('/teachers');
}

// LR-073 — admin CRUD for the Teacher entity itself (distinct from the
// Teacher.id resolution getTeachers() above already supports for
// Group/Workshop forms).
export interface TeacherRequestDTO {
	firstName: string;
	lastName: string;
	email: string;
	phone: string;
	title: string;
	approved: boolean;
	bioDe: string;
	bioEn: string;
	bioUa: string;
	active: boolean;
}

export function createTeacher(input: TeacherRequestDTO) {
	return authRequest<TeacherInfoDTO>('/teachers', { method: 'POST', body: JSON.stringify(input) });
}

export function updateTeacher(id: number, input: TeacherRequestDTO) {
	return authRequest<TeacherInfoDTO>(`/teachers/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function deleteTeacher(id: number) {
	return authRequest<void>(`/teachers/${id}`, { method: 'DELETE' });
}

// ----- Groups -----
// GroupController takes the raw JPA entity as @RequestBody (no DTO) — this
// shape matches Group.java's writable fields exactly. `teacher` here MUST be
// a Teacher.id (from getTeachers() above), NOT a User.id — the old static
// site's admin-groups.js populated this select from /users/role/TEACHER and
// sent User.id, silently linking groups to the wrong teacher row or none at
// all. Fixed here, see CHANGELOG.md 2026-07-23.
// (GroupDTO itself is defined earlier, in the public-catalog section.)

export interface GroupWriteDTO {
	titleDe: string;
	titleEn: string;
	titleUa: string;
	capacity: number;
	startDateTime: string;
	endDateTime: string | null;
	workshop: { id: number } | null;
	teacher: { id: number } | null;
	activity: { id: number } | null;
	// LR-015 — where this session happens; venue moved here from Workshop.
	venue: { id: number } | null;
	ageGroup: { id: number } | null;
	active: boolean;
}

// LR-030 — POST /groups no longer binds the raw entity (backend:
// GroupCreateDTO) — flat ids, not nested {id} objects like GroupWriteDTO
// above (still correct for PUT, which the raw-entity-bound update()
// endpoint is unaffected by this fix and still expects).
export interface GroupCreateRequestDTO {
	titleDe: string;
	titleEn: string;
	titleUa: string;
	capacity: number;
	startDateTime: string;
	endDateTime: string | null;
	workshopId: number | null;
	teacherId: number | null;
	activityId: number | null;
	venueId: number | null;
	ageGroupId: number | null;
	active: boolean;
}

// Same authRequest correction as getVenues() above — GET /groups requires
// a valid JWT under the current SecurityConfig even though it has no
// @PreAuthorize of its own.
export function getGroups(workshopId?: number) {
	return authRequest<GroupDTO[]>(`/groups${workshopId ? `?workshopId=${workshopId}` : ''}`);
}

export function createGroup(input: GroupCreateRequestDTO) {
	return authRequest<GroupDTO>('/groups', { method: 'POST', body: JSON.stringify(input) });
}

export function updateGroup(id: number, input: GroupWriteDTO) {
	return authRequest<GroupDTO>(`/groups/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function deleteGroup(id: number) {
	return authRequest<void>(`/groups/${id}`, { method: 'DELETE' });
}

// ----- Sessions (LR-067 backend, LR-074 controller) -----
// Child of Group, not a standalone resource — multi-day schedule, one
// row per day. Replacing the list resubmits every day each time, not
// incremental add/remove (matches the admin form's "number of days" UX).

export interface SessionDTO {
	id: number;
	startDateTime: string;
	endDateTime: string | null;
	venueId: number | null;
	venueName: string | null;
}

export interface SessionWriteDTO {
	startDateTime: string;
	endDateTime: string | null;
	venueId: number | null;
}

export function getSessions(groupId: number) {
	return authRequest<SessionDTO[]>(`/groups/${groupId}/sessions`);
}

export function replaceSessions(groupId: number, input: SessionWriteDTO[]) {
	return authRequest<SessionDTO[]>(`/groups/${groupId}/sessions`, { method: 'PUT', body: JSON.stringify(input) });
}

// teacherId here = Teacher.id (see note above) — do not pass a User.id.
export function getGroupsByTeacherId(teacherId: number) {
	return authRequest<GroupDTO[]>(`/groups/teacher/${teacherId}`);
}

// ----- Performances (admin) -----
// `venue` is a plain free-text string on the real backend DTO, not a Venue
// FK — the old static site's admin-performances.js sent `venueId` (a field
// the backend silently ignores, ID never actually applied) and read
// `p.date`/`p.venueName` on responses (neither field exists — real names
// are `performanceDate`/`venue`). Fixed here, see CHANGELOG.md 2026-07-23.

export type PerformanceStatus = 'PLANNED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface PerformanceWriteDTO {
	workshopId: number | null;
	title: string;
	description: string;
	performanceDate: string;
	venue: string;
	maxAttendees: number | null;
	status: PerformanceStatus;
}

export function createPerformance(input: PerformanceWriteDTO) {
	return authRequest<PerformanceDTO>('/performances', { method: 'POST', body: JSON.stringify(input) });
}

export function updatePerformance(id: number, input: PerformanceWriteDTO) {
	return authRequest<PerformanceDTO>(`/performances/${id}`, { method: 'PUT', body: JSON.stringify(input) });
}

export function deletePerformance(id: number) {
	return authRequest<void>(`/performances/${id}`, { method: 'DELETE' });
}

// ----- Teacher-scoped reads (teacher dashboard) -----

export interface EnrollmentAdminDTO {
	id: number;
	workshopId: number;
	workshopTitle: string;
	groupId: number;
	groupTitle: string;
	status: string;
	createdAt: string;
	user: UserBasicDTO;
}

export function getGroupParticipants(groupId: number) {
	return authRequest<EnrollmentAdminDTO[]>(`/teacher/groups/${groupId}/participants`);
}
