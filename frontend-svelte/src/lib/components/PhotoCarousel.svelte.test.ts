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

	it('renders media console controls (REW, PLAY/PAUSE, FWD)', () => {
		const { getByRole } = render(PhotoCarousel);
		const toolbar = getByRole('toolbar', { name: /player controls/i });
		expect(toolbar).toBeInTheDocument();

		const buttons = toolbar.querySelectorAll('button');
		expect(buttons.length).toBe(3);
		expect(buttons[0]).toHaveTextContent('REW');
		expect(buttons[1]).toHaveTextContent('PAUSE');
		expect(buttons[2]).toHaveTextContent('FWD');
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
		const toolbar = getByRole('toolbar', { name: /player controls/i });
		const playPauseBtn = toolbar.querySelectorAll('button')[1];

		expect(playPauseBtn).toHaveTextContent('PAUSE');

		// Click to pause
		await user.click(playPauseBtn);
		expect(playPauseBtn).toHaveTextContent('PLAY');

		// Click to resume play
		await user.click(playPauseBtn);
		expect(playPauseBtn).toHaveTextContent('PAUSE');
	});

	it('advances active indicator when FWD button is clicked', async () => {
		const user = userEvent.setup();
		const { getAllByRole, getByRole } = render(PhotoCarousel);
		const toolbar = getByRole('toolbar', { name: /player controls/i });
		const fwdBtn = toolbar.querySelectorAll('button')[2];

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
