<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import * as m from '$lib/paraglide/messages.js';
	import { login, register, persistSession, ApiError } from '$lib/api';

	let loginEmail = $state('');
	let loginPassword = $state('');
	let loginBusy = $state(false);
	let loginError = $state('');

	let regFirstName = $state('');
	let regLastName = $state('');
	let regEmail = $state('');
	let regPassword = $state('');
	let regConfirm = $state('');
	let acceptTerms = $state(false);
	let acceptPrivacy = $state(false);
	let regBusy = $state(false);
	let regError = $state('');

	function redirectForRole(role: string) {
		const target = role === 'ADMIN' ? '/admin' : role === 'TEACHER' ? '/teacher' : '/dashboard';
		goto(target);
	}

	async function handleLogin(e: SubmitEvent) {
		e.preventDefault();
		loginError = '';
		loginBusy = true;
		try {
			const data = await login(loginEmail, loginPassword);
			persistSession(data);
			redirectForRole(data.role);
		} catch (err) {
			loginError =
				err instanceof ApiError && err.status === 401
					? 'E-Mail oder Passwort falsch.'
					: (err as Error).message;
		} finally {
			loginBusy = false;
		}
	}

	async function handleRegister(e: SubmitEvent) {
		e.preventDefault();
		regError = '';
		if (!acceptTerms || !acceptPrivacy) {
			regError = 'Bitte Bedingungen akzeptieren.';
			return;
		}
		if (regPassword !== regConfirm) {
			regError = 'Passwörter stimmen nicht überein.';
			return;
		}
		regBusy = true;
		try {
			const data = await register({
				firstName: regFirstName,
				lastName: regLastName,
				email: regEmail,
				password: regPassword
			});
			persistSession(data);
			redirectForRole(data.role);
		} catch (err) {
			regError = (err as Error).message;
		} finally {
			regBusy = false;
		}
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.login_title()}</title>
</svelte:head>

<section class="mx-auto grid max-w-4xl gap-10 px-6 py-16 sm:py-24 md:grid-cols-2 md:gap-14">
	<!-- Login -->
	<form onsubmit={handleLogin} class="rounded-2xl border border-ink-line bg-ink-soft/40 p-8">
		<h1 class="font-display text-2xl font-semibold text-paper">{m.login_title()}</h1>

		<label class="mt-6 block text-sm text-paper-dim" for="loginEmail">{m.login_email_label()}</label>
		<input
			id="loginEmail"
			type="email"
			required
			bind:value={loginEmail}
			class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
		/>

		<label class="mt-4 block text-sm text-paper-dim" for="loginPassword">{m.login_password_label()}</label>
		<input
			id="loginPassword"
			type="password"
			required
			bind:value={loginPassword}
			class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-gold"
		/>

		{#if loginError}
			<p class="mt-3 text-sm text-gold">{loginError}</p>
		{/if}

		<button
			type="submit"
			disabled={loginBusy}
			class="mt-6 w-full rounded-full bg-gold py-3 font-display font-semibold text-ink transition-colors hover:bg-gold-deep disabled:opacity-60"
		>
			{loginBusy ? '…' : m.login_submit()}
		</button>

		<!-- Password-reset flow doesn't exist on the backend yet (ARCHITECTURE_OLD.md
		     finding) — link stays a placeholder until that ticket lands. -->
		<p class="mt-4 text-center text-sm text-paper-dim">{m.login_forgot_password()}</p>
	</form>

	<!-- Register -->
	<form onsubmit={handleRegister} class="rounded-2xl border border-ink-line bg-ink-soft/40 p-8">
		<h2 class="font-display text-2xl font-semibold text-paper">{m.register_title()}</h2>

		<div class="mt-6 grid grid-cols-2 gap-4">
			<div>
				<label class="block text-sm text-paper-dim" for="regFirstName">{m.register_firstname_label()}</label>
				<input
					id="regFirstName"
					required
					bind:value={regFirstName}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-teal"
				/>
			</div>
			<div>
				<label class="block text-sm text-paper-dim" for="regLastName">{m.register_lastname_label()}</label>
				<input
					id="regLastName"
					required
					bind:value={regLastName}
					class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-teal"
				/>
			</div>
		</div>

		<label class="mt-4 block text-sm text-paper-dim" for="regEmail">{m.register_email_label()}</label>
		<input
			id="regEmail"
			type="email"
			required
			bind:value={regEmail}
			class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-teal"
		/>

		<label class="mt-4 block text-sm text-paper-dim" for="regPassword">{m.register_password_label()}</label>
		<input
			id="regPassword"
			type="password"
			required
			minlength="6"
			bind:value={regPassword}
			class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-teal"
		/>

		<label class="mt-4 block text-sm text-paper-dim" for="regConfirm">{m.register_confirm_label()}</label>
		<input
			id="regConfirm"
			type="password"
			required
			bind:value={regConfirm}
			class="mt-1 w-full rounded-lg border border-ink-line bg-ink px-4 py-2.5 text-paper outline-none focus:border-teal"
		/>

		<label class="mt-4 flex items-start gap-2 text-sm text-paper-dim">
			<input type="checkbox" required bind:checked={acceptTerms} class="mt-1 accent-teal" />
			{m.register_terms_label()}
		</label>
		<label class="mt-2 flex items-start gap-2 text-sm text-paper-dim">
			<input type="checkbox" required bind:checked={acceptPrivacy} class="mt-1 accent-teal" />
			{m.register_privacy_label()}
		</label>

		{#if regError}
			<p class="mt-3 text-sm text-gold">{regError}</p>
		{/if}

		<button
			type="submit"
			disabled={regBusy}
			class="mt-6 w-full rounded-full bg-teal py-3 font-display font-semibold text-ink transition-colors hover:bg-teal-deep disabled:opacity-60"
		>
			{regBusy ? '…' : m.register_submit()}
		</button>
		<p class="mt-3 text-center text-xs text-paper-dim">{m.required_note()}</p>
	</form>
</section>
