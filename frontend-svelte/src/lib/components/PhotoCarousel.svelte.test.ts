import { render } from '@testing-library/svelte';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import PhotoCarousel from './PhotoCarousel.svelte';

describe('PhotoCarousel', () => {
	it('renders carousel with region role and accessible label', () => {
		const { getByRole } = render(PhotoCarousel);
		const region = getByRole('region');
		expect(region).toBeInTheDocument();
		expect(region).toHaveAttribute('aria-roledescription', 'carousel');
	});

	it('starts with the orange suitcase photo as the primary active slide', () => {
		const { container } = render(PhotoCarousel);
		const images = container.querySelectorAll<HTMLImageElement>('.carousel-slide img');
		// extended tape has 6 elements: [Clone of 3, Slide 0, Slide 1, Slide 2, Slide 3, Clone of 0]
		expect(images.length).toBe(6);

		// Slide 0 (index 1 in extended tape) must be 01-theatre-action.png (orange suitcase on the right)
		expect(images[1].getAttribute('src')).toBe('/images/carousel/01-theatre-action.png');
		expect(images[1].getAttribute('loading')).toBe('eager');
	});

	it('renders theatre navigation controls (previous, next, play/pause)', () => {
		const { getAllByRole, getByRole } = render(PhotoCarousel);
		const prevButtons = getAllByRole('button', { name: /vorheriges foto/i });
		const nextButtons = getAllByRole('button', { name: /nächstes foto/i });
		const playPauseBtn = getByRole('button', { name: /diashow anhalten|diashow abspielen/i });

		expect(prevButtons.length).toBeGreaterThan(0);
		expect(nextButtons.length).toBeGreaterThan(0);
		expect(playPauseBtn).toBeInTheDocument();
	});

	it('renders 4 slide indicators with the first one active initially', () => {
		const { getAllByRole } = render(PhotoCarousel);
		const tabs = getAllByRole('tab');
		expect(tabs.length).toBe(4);
		expect(tabs[0]).toHaveAttribute('aria-selected', 'true');
		expect(tabs[1]).toHaveAttribute('aria-selected', 'false');
		expect(tabs[2]).toHaveAttribute('aria-selected', 'false');
		expect(tabs[3]).toHaveAttribute('aria-selected', 'false');
	});

	it('toggles playback when PLAY/PAUSE button is clicked', async () => {
		const user = userEvent.setup();
		const { getByRole } = render(PhotoCarousel);
		const playPauseBtn = getByRole('button', { name: /diashow anhalten/i });

		expect(playPauseBtn).toHaveTextContent(/pause/i);

		// Click to pause
		await user.click(playPauseBtn);
		expect(playPauseBtn).toHaveTextContent(/play/i);

		// Click to resume play
		await user.click(playPauseBtn);
		expect(playPauseBtn).toHaveTextContent(/pause/i);
	});

	it('advances active indicator when Next button is clicked', async () => {
		const user = userEvent.setup();
		const { getAllByRole } = render(PhotoCarousel);
		const nextButtons = getAllByRole('button', { name: /nächstes foto/i });
		const fwdBtn = nextButtons[0];

		await user.click(fwdBtn);

		const tabs = getAllByRole('tab');
		expect(tabs[1]).toHaveAttribute('aria-selected', 'true');
		expect(tabs[0]).toHaveAttribute('aria-selected', 'false');
	});

	it('switches slide when an indicator dot is clicked', async () => {
		const user = userEvent.setup();
		const { getAllByRole } = render(PhotoCarousel);
		const tabs = getAllByRole('tab');

		await user.click(tabs[2]);
		expect(tabs[2]).toHaveAttribute('aria-selected', 'true');
		expect(tabs[0]).toHaveAttribute('aria-selected', 'false');
	});
});
