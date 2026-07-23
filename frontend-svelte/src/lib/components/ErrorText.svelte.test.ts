import { render } from '@testing-library/svelte';
import { describe, expect, it } from 'vitest';
import ErrorText from './ErrorText.svelte';

describe('ErrorText', () => {
	it('renders nothing when there is no message', () => {
		// Svelte leaves an internal `<!---->` anchor comment for the false
		// branch of {#if} — toBeEmptyDOMElement() treats that as "not empty",
		// so assert on visible content/role instead of raw DOM emptiness.
		const { container, queryByRole } = render(ErrorText, { props: { message: '' } });
		expect(container).toHaveTextContent('');
		expect(queryByRole('alert')).not.toBeInTheDocument();
	});

	it('renders the message as an alert when present', () => {
		const { getByRole } = render(ErrorText, { props: { message: 'E-Mail oder Passwort falsch.' } });
		expect(getByRole('alert')).toHaveTextContent('E-Mail oder Passwort falsch.');
	});

	it('uses the semantic error color, not a brand accent', () => {
		const { getByRole } = render(ErrorText, { props: { message: 'x' } });
		// Regression test for the exact bug the reviewer found: error text
		// used to reuse `text-gold` (also the login CTA color).
		expect(getByRole('alert')).toHaveClass('text-error');
		expect(getByRole('alert')).not.toHaveClass('text-gold');
	});
});
