<script lang="ts">
	import { page } from '$app/state';
	import * as m from '$lib/paraglide/messages.js';
	import { isAuthenticated, enrollInWorkshop, enrollInCourse, ApiError, type EnrollmentDTO } from '$lib/api';
	import Button from './Button.svelte';

	// LR-084 (roundtable, Rich Harris) — the login-gate + API-dispatch +
	// error-handling logic was about to be copy-pasted a second time for
	// Course after already existing once for Workshop; factored out here.
	// Layout stays with the caller (Workshop shows a list of Groups to pick
	// from, Course has at most one — see enrollCourse's auto-resolve) —
	// only the action itself is shared.
	let {
		targetType,
		targetId,
		groupId,
		disabled = false,
		label,
		onSuccess,
		onError
	}: {
		targetType: 'workshop' | 'course';
		targetId: number;
		groupId?: number;
		disabled?: boolean;
		label: string;
		onSuccess: (result: EnrollmentDTO) => void;
		onError: (message: string) => void;
	} = $props();

	let busy = $state(false);

	async function handleClick() {
		if (!isAuthenticated()) {
			// Full navigation, not goto() — same reasoning as the dashboard's
			// own auth redirect: we're leaving the current (unauthenticated)
			// session state behind entirely, not doing an SPA-internal move.
			const returnTo = encodeURIComponent(page.url.pathname);
			window.location.href = `/login?returnTo=${returnTo}`;
			return;
		}

		busy = true;
		try {
			const result =
				targetType === 'workshop' ? await enrollInWorkshop(targetId, groupId) : await enrollInCourse(targetId);
			onSuccess(result);
		} catch (err) {
			onError(translateEnrollError(err));
		} finally {
			busy = false;
		}
	}

	// architect-reviewer, 2026-08-16 — GROUP_FULL/ALREADY_ENROLLED are real,
	// expected outcomes (not server errors) with their own `code` from
	// GlobalExceptionHandler — this is Course's everyday full-course path,
	// not just a rare Workshop race, so it needs a clean translated
	// message, not the generic fallback.
	function translateEnrollError(err: unknown): string {
		if (err instanceof ApiError) {
			if (err.code === 'GROUP_FULL') return m.enroll_group_full();
			if (err.code === 'ALREADY_ENROLLED') return m.enroll_already_enrolled();
			return err.message;
		}
		return m.workshop_detail_enroll_generic_error();
	}
</script>

<Button variant="teal" fullWidth={false} {disabled} {busy} onclick={handleClick}>
	{label}
</Button>
