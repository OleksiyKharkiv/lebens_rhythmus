import { render } from '@testing-library/svelte';
import userEvent from '@testing-library/user-event';
import { createRawSnippet } from 'svelte';
import { describe, expect, it, vi } from 'vitest';
import Select from './Select.svelte';

function optionsSnippet() {
	return createRawSnippet(() => ({
		render: () => `
			<optgroup label="options">
				<option value="">—</option>
				<option value="opt1">Option 1</option>
				<option value="opt2">Option 2</option>
			</optgroup>
		`
	}));
}

describe('Select', () => {
	it('renders label when provided and links it to select via id', () => {
		const { getByLabelText, getByRole } = render(Select, {
			props: {
				id: 'test-select',
				label: 'Choose Option',
				children: optionsSnippet()
			}
		});

		const select = getByRole('combobox');
		expect(select).toBeInTheDocument();
		expect(getByLabelText('Choose Option')).toBe(select);
	});

	it('uses ariaLabel when label is not provided', () => {
		const { getByRole } = render(Select, {
			props: {
				id: 'role-select',
				ariaLabel: 'User Role',
				children: optionsSnippet()
			}
		});

		const select = getByRole('combobox', { name: 'User Role' });
		expect(select).toBeInTheDocument();
	});

	it('calls onchange with the selected string value', async () => {
		const onchange = vi.fn();
		const user = userEvent.setup();
		const { getByRole } = render(Select, {
			props: {
				id: 'test-change',
				label: 'Pick',
				onchange,
				children: optionsSnippet()
			}
		});

		const select = getByRole('combobox');
		await user.selectOptions(select, 'opt2');

		expect(onchange).toHaveBeenCalledWith('opt2');
	});

	it('respects the disabled attribute', () => {
		const { getByRole } = render(Select, {
			props: {
				id: 'disabled-select',
				label: 'Disabled',
				disabled: true,
				children: optionsSnippet()
			}
		});

		const select = getByRole('combobox');
		expect(select).toBeDisabled();
	});
});
