<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import * as m from '$lib/paraglide/messages.js';
	import { login, register, persistSession, ApiError } from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Input from '$lib/components/Input.svelte';
	import Button from '$lib/components/Button.svelte';
	import ErrorText from '$lib/components/ErrorText.svelte';

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
	<Card>
		<form onsubmit={handleLogin}>
			<h1 class="font-display text-2xl font-semibold text-paper">{m.login_title()}</h1>

			<Input id="loginEmail" label={m.login_email_label()} type="email" required bind:value={loginEmail} />
			<Input
				id="loginPassword"
				label={m.login_password_label()}
				type="password"
				required
				bind:value={loginPassword}
			/>

			<ErrorText message={loginError} />

			<div class="mt-6">
				<Button type="submit" busy={loginBusy}>{m.login_submit()}</Button>
			</div>

			<!-- Password-reset flow doesn't exist on the backend yet (ARCHITECTURE_OLD.md
			     finding) — link stays a placeholder until that ticket lands. -->
			<p class="mt-4 text-center text-sm text-paper-dim">{m.login_forgot_password()}</p>
		</form>
	</Card>

	<!-- Register -->
	<Card>
		<form onsubmit={handleRegister}>
			<h2 class="font-display text-2xl font-semibold text-paper">{m.register_title()}</h2>

			<div class="mt-6 grid grid-cols-2 gap-4">
				<Input
					id="regFirstName"
					label={m.register_firstname_label()}
					accent="teal"
					required
					bind:value={regFirstName}
				/>
				<Input
					id="regLastName"
					label={m.register_lastname_label()}
					accent="teal"
					required
					bind:value={regLastName}
				/>
			</div>

			<Input
				id="regEmail"
				label={m.register_email_label()}
				type="email"
				accent="teal"
				required
				bind:value={regEmail}
			/>
			<Input
				id="regPassword"
				label={m.register_password_label()}
				type="password"
				accent="teal"
				required
				minlength={6}
				bind:value={regPassword}
			/>
			<Input
				id="regConfirm"
				label={m.register_confirm_label()}
				type="password"
				accent="teal"
				required
				bind:value={regConfirm}
			/>

			<label class="mt-4 flex items-start gap-2 text-sm text-paper-dim">
				<input type="checkbox" required bind:checked={acceptTerms} class="mt-1 accent-teal" />
				{m.register_terms_label()}
			</label>
			<label class="mt-2 flex items-start gap-2 text-sm text-paper-dim">
				<input type="checkbox" required bind:checked={acceptPrivacy} class="mt-1 accent-teal" />
				{m.register_privacy_label()}
			</label>

			<ErrorText message={regError} />

			<div class="mt-6">
				<Button type="submit" variant="teal" busy={regBusy}>{m.register_submit()}</Button>
			</div>
			<p class="mt-3 text-center text-xs text-paper-dim">{m.required_note()}</p>
		</form>
	</Card>
</section>
