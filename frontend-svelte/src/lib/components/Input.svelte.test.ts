import { cleanup, render } from '@testing-library/svelte';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import Input from './Input.svelte';

describe('Input', () => {
	it('associates the label with the input via id/for', () => {
		const { getByLabelText } = render(Input, { props: { id: 'loginEmail', label: 'E-Mail' } });
		expect(getByLabelText('E-Mail')).toHaveAttribute('id', 'loginEmail');
	});

	it('updates the bound value as the user types', async () => {
		const user = userEvent.setup();
		const { getByLabelText } = render(Input, { props: { id: 'x', label: 'Name' } });
		const input = getByLabelText('Name');
		await user.type(input, 'Olena');
		expect(input).toHaveValue('Olena');
	});

	it('forwards required and minlength to the underlying input', () => {
		const { getByLabelText } = render(Input, {
			props: { id: 'regPassword', label: 'Passwort', required: true, minlength: 6 }
		});
		// required:true appends a visible "*" to the label text (found live,
		// 2026-08-13 — beta feedback: required fields had no marker at all).
		// The asterisk is aria-hidden, so a real screen reader still
		// announces just "Passwort" — but @testing-library/dom's
		// getByLabelText matches the label's raw textContent for `for`
		// association, not the full ARIA accessible-name algorithm, so the
		// query itself must account for it.
		const input = getByLabelText(/Passwort/);
		expect(input).toBeRequired();
		expect(input).toHaveAttribute('minlength', '6');
	});

	it('shows a required marker in the label when required, not otherwise', () => {
		const required = render(Input, { props: { id: 'a', label: 'Feld', required: true } });
		expect(required.getByText('*')).toBeInTheDocument();
		// render() mounts into the shared document.body with no automatic
		// cleanup between calls in the same test (no global afterEach(cleanup)
		// configured in this project) — without this, the second render's
		// query would still see the first render's "*" and pass for the
		// wrong reason.
		cleanup();

		const optional = render(Input, { props: { id: 'b', label: 'Feld' } });
		expect(optional.queryByText('*')).not.toBeInTheDocument();
	});
});
