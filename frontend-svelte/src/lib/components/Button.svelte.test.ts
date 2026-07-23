import { render } from '@testing-library/svelte';
import userEvent from '@testing-library/user-event';
import { createRawSnippet } from 'svelte';
import { describe, expect, it, vi } from 'vitest';
import Button from './Button.svelte';

// Button/Input/Card/Card.svelte all accept a `Snippet` for their content —
// createRawSnippet is Svelte's documented way to construct one outside of an
// actual .svelte template, so these tests don't need a throwaway host component.
function textSnippet(text: string) {
	return createRawSnippet(() => ({ render: () => `<span>${text}</span>` }));
}

describe('Button', () => {
	it('renders its label', () => {
		const { getByRole } = render(Button, { props: { children: textSnippet('Anmelden') } });
		expect(getByRole('button')).toHaveTextContent('Anmelden');
	});

	it('disables and shows the busy indicator while busy', () => {
		const { getByRole } = render(Button, {
			props: { busy: true, children: textSnippet('Anmelden') }
		});
		const button = getByRole('button');
		expect(button).toBeDisabled();
		expect(button).toHaveTextContent('…');
	});

	it('applies the gold variant by default', () => {
		const { getByRole } = render(Button, { props: { children: textSnippet('x') } });
		expect(getByRole('button')).toHaveClass('bg-gold');
	});

	it('applies the teal variant when requested', () => {
		const { getByRole } = render(Button, {
			props: { variant: 'teal', children: textSnippet('x') }
		});
		expect(getByRole('button')).toHaveClass('bg-teal');
	});

	it('calls onclick when clicked and not disabled', async () => {
		const onclick = vi.fn();
		const user = userEvent.setup();
		const { getByRole } = render(Button, {
			props: { onclick, children: textSnippet('x') }
		});
		await user.click(getByRole('button'));
		expect(onclick).toHaveBeenCalledOnce();
	});

	it('is not clickable while disabled', async () => {
		const onclick = vi.fn();
		const user = userEvent.setup();
		const { getByRole } = render(Button, {
			props: { onclick, disabled: true, children: textSnippet('x') }
		});
		await user.click(getByRole('button'));
		expect(onclick).not.toHaveBeenCalled();
	});
});
