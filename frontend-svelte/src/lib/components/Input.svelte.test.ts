import { render } from '@testing-library/svelte';
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
		const input = getByLabelText('Passwort');
		expect(input).toBeRequired();
		expect(input).toHaveAttribute('minlength', '6');
	});
});
