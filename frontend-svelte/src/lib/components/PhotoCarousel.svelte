<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import * as m from '$lib/paraglide/messages.js';

	const AUTOPLAY_INTERVAL = 5000;

	// Carousel items definition with localized strings
	const slides = [
		{
			id: 'theatre-action',
			src: '/images/carousel/01-theatre-action.png',
			title: () => m.home_carousel_slide1_title(),
			tag: () => m.home_carousel_slide1_tag()
		},
		{
			id: 'theatre-rehearsal',
			src: '/images/carousel/02-theatre-rehearsal.jpg',
			title: () => m.home_carousel_slide2_title(),
			tag: () => m.home_carousel_slide2_tag()
		},
		{
			id: 'theatre-ensemble',
			src: '/images/carousel/03-theatre-ensemble.jpg',
			title: () => m.home_carousel_slide3_title(),
			tag: () => m.home_carousel_slide3_tag()
		},
		{
			id: 'theatre-applause',
			src: '/images/carousel/04-theatre-applause.jpg',
			title: () => m.home_carousel_slide4_title(),
			tag: () => m.home_carousel_slide4_tag()
		}
	];

	// Extended tape for seamless circular sliding without backward rewind:
	// [Clone of 3, Slide 0, Slide 1, Slide 2, Slide 3, Clone of 0]
	const extendedSlides = [slides[slides.length - 1], ...slides, slides[0]];

	let trackIndex = $state(1); // 1 = Real Slide 0 (01-theatre-action.png)
	let activeSlideIndex = $state(0); // 0..3 for dots and captions
	let isPlaying = $state(true);
	let isHovered = $state(false);
	let withTransition = $state(true);
	let isAnimating = $state(false);

	let autoplayTimer: ReturnType<typeof setInterval> | null = null;
	let animationSafetyTimer: ReturnType<typeof setTimeout> | null = null;

	// Instagram-style Tap-to-Pause feedback
	let showTapFeedback = $state(false);
	let tapFeedbackIcon = $state<'play' | 'pause'>('pause');
	let tapFeedbackTimer: ReturnType<typeof setTimeout> | null = null;

	// Touch & swipe coordinates
	let touchStartX = 0;
	let touchStartY = 0;
	let touchStartTime = 0;
	let isSwiping = false;
	let lastTouchEndTime = 0;

	function startAutoplay() {
		stopAutoplay();
		if (isPlaying && !isHovered) {
			autoplayTimer = setInterval(() => {
				nextSlide();
			}, AUTOPLAY_INTERVAL);
		}
	}

	function stopAutoplay() {
		if (autoplayTimer) {
			clearInterval(autoplayTimer);
			autoplayTimer = null;
		}
	}

	function triggerTapFeedback(type: 'play' | 'pause') {
		tapFeedbackIcon = type;
		showTapFeedback = true;
		if (tapFeedbackTimer) clearTimeout(tapFeedbackTimer);
		tapFeedbackTimer = setTimeout(() => {
			showTapFeedback = false;
		}, 650);
	}

	function togglePlayPause() {
		isPlaying = !isPlaying;
		isHovered = false;
		triggerTapFeedback(isPlaying ? 'play' : 'pause');
		if (isPlaying) {
			startAutoplay();
		} else {
			stopAutoplay();
		}
	}

	function armAnimationSafety() {
		if (animationSafetyTimer) clearTimeout(animationSafetyTimer);
		animationSafetyTimer = setTimeout(() => {
			if (isAnimating) {
				handleTransitionEnd();
			}
		}, 850);
	}

	function nextSlide() {
		if (isAnimating) return;
		isAnimating = true;
		armAnimationSafety();
		withTransition = true;
		trackIndex++;
		activeSlideIndex = (activeSlideIndex + 1) % slides.length;
	}

	function prevSlide() {
		if (isAnimating) return;
		isAnimating = true;
		armAnimationSafety();
		withTransition = true;
		trackIndex--;
		activeSlideIndex = (activeSlideIndex - 1 + slides.length) % slides.length;
	}

	function handleManualNext() {
		nextSlide();
		if (isPlaying) startAutoplay();
	}

	function handleManualPrev() {
		prevSlide();
		if (isPlaying) startAutoplay();
	}

	function selectSlide(index: number) {
		if (activeSlideIndex === index || isAnimating) return;
		withTransition = true;
		isAnimating = true;
		armAnimationSafety();
		activeSlideIndex = index;
		trackIndex = index + 1;
		if (isPlaying) startAutoplay();
	}

	function handleTransitionEnd(event?: TransitionEvent) {
		if (event) {
			if (event.target !== event.currentTarget) return;
			if (event.propertyName !== 'transform') return;
		}
		if (animationSafetyTimer) {
			clearTimeout(animationSafetyTimer);
			animationSafetyTimer = null;
		}
		isAnimating = false;

		// Reached appended clone of Slide 0 at index 5 -> snap silently to real Slide 0 at index 1
		if (trackIndex === extendedSlides.length - 1) {
			withTransition = false;
			trackIndex = 1;
			requestAnimationFrame(() => {
				requestAnimationFrame(() => {
					withTransition = true;
				});
			});
		}
		// Reached prepended clone of Slide 3 at index 0 -> snap silently to real Slide 3 at index 4
		else if (trackIndex === 0) {
			withTransition = false;
			trackIndex = slides.length;
			requestAnimationFrame(() => {
				requestAnimationFrame(() => {
					withTransition = true;
				});
			});
		}
	}

	function handleMouseEnter() {
		if (typeof window !== 'undefined' && window.matchMedia('(hover: hover)').matches) {
			isHovered = true;
			stopAutoplay();
		}
	}

	function handleMouseLeave() {
		isHovered = false;
		if (isPlaying) startAutoplay();
	}

	function handleTouchStart(e: TouchEvent) {
		if (e.touches.length !== 1) return;
		touchStartX = e.touches[0].clientX;
		touchStartY = e.touches[0].clientY;
		touchStartTime = Date.now();
		isSwiping = true;
	}

	function handleTouchEnd(e: TouchEvent) {
		if (!isSwiping) return;
		isSwiping = false;
		lastTouchEndTime = Date.now();
		const touch = e.changedTouches[0];
		if (!touch) return;

		const deltaX = touch.clientX - touchStartX;
		const deltaY = touch.clientY - touchStartY;
		const duration = Date.now() - touchStartTime;
		const absX = Math.abs(deltaX);
		const absY = Math.abs(deltaY);

		// Horizontal swipe: >= 40px, predominantly horizontal
		if (absX > 40 && absX > absY * 1.2 && duration < 800) {
			if (deltaX < 0) {
				handleManualNext();
			} else {
				handleManualPrev();
			}
			return;
		}

		// Quick tap: < 15px movement and < 300ms duration
		if (absX < 15 && absY < 15 && duration < 300) {
			togglePlayPause();
		}
	}

	function handleSlideClick() {
		if (Date.now() - lastTouchEndTime < 400) return;
		togglePlayPause();
	}

	function handleVisibilityChange() {
		if (document.hidden) {
			stopAutoplay();
		} else {
			if (isPlaying) {
				isAnimating = false;
				startAutoplay();
			}
		}
	}

	onMount(() => {
		isHovered = false;
		isAnimating = false;
		startAutoplay();
		document.addEventListener('visibilitychange', handleVisibilityChange);
	});

	onDestroy(() => {
		stopAutoplay();
		if (animationSafetyTimer) clearTimeout(animationSafetyTimer);
		if (tapFeedbackTimer) clearTimeout(tapFeedbackTimer);
		if (typeof document !== 'undefined') {
			document.removeEventListener('visibilitychange', handleVisibilityChange);
		}
	});
</script>

<div
	class="carousel-root flex flex-col items-center w-full"
	role="region"
	aria-roledescription="carousel"
	aria-label={m.home_carousel_aria()}
>
	<!-- Outer Viewport Frame -->
	<div
		class="carousel-viewport-mask relative aspect-[4/3] w-full overflow-hidden rounded-[2rem] border border-ink-line bg-ink shadow-2xl focus:outline-none focus-visible:ring-2 focus-visible:ring-gold"
		onmouseenter={handleMouseEnter}
		onmouseleave={handleMouseLeave}
		ontouchstart={handleTouchStart}
		ontouchend={handleTouchEnd}
		onclick={handleSlideClick}
		role="button"
		tabindex="0"
		aria-label={isPlaying ? m.home_carousel_tap_pause() : m.home_carousel_tap_play()}
		onkeydown={(e) => {
			if (e.key === ' ' || e.key === 'Enter') {
				e.preventDefault();
				togglePlayPause();
			} else if (e.key === 'ArrowRight') {
				e.preventDefault();
				handleManualNext();
			} else if (e.key === 'ArrowLeft') {
				e.preventDefault();
				handleManualPrev();
			}
		}}
	>
		<!-- Subtle ambient background glow -->
		<div
			class="absolute -inset-1 opacity-20 blur-xl pointer-events-none rounded-[2rem]"
			style="background: radial-gradient(circle at 50% 50%, var(--color-gold) 0%, var(--color-teal) 60%, transparent 80%);"
			aria-hidden="true"
		></div>

		<!-- Floating Tap/Click Feedback Badge -->
		{#if showTapFeedback}
			<div class="tap-feedback-badge" aria-hidden="true">
				{#if tapFeedbackIcon === 'play'}
					<svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
						<polygon points="6 4 20 12 6 20 6 4" />
					</svg>
				{:else}
					<svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
						<rect x="6" y="4" width="4" height="16" rx="1.5" />
						<rect x="14" y="4" width="4" height="16" rx="1.5" />
					</svg>
				{/if}
			</div>
		{/if}

		<!-- Sliding Film Track -->
		<div
			class="carousel-track h-full"
			class:no-transition={!withTransition}
			style="transform: translateX(-{trackIndex * 100}%);"
			ontransitionend={handleTransitionEnd}
		>
			{#each extendedSlides as slide, idx (idx)}
				<div class="carousel-slide relative h-full flex-shrink-0">
					<img
						src={slide.src}
						alt={slide.title()}
						class="h-full w-full object-cover object-center select-none"
						draggable="false"
						loading={idx === 1 ? 'eager' : 'lazy'}
					/>

					<!-- Subtle Vignette & Gradient for text contrast -->
					<div
						class="absolute inset-0 bg-gradient-to-t from-ink/90 via-ink/20 to-transparent pointer-events-none"
						aria-hidden="true"
					></div>

					<!-- Slide Caption Overlay -->
					<div
						class="absolute bottom-3 left-3 right-3 sm:bottom-4 sm:left-4 sm:right-4 flex items-center justify-between gap-3 rounded-2xl bg-ink/75 backdrop-blur-md px-3.5 py-2.5 sm:px-4 sm:py-3 border border-white/10"
					>
						<div class="flex items-center gap-2.5 min-w-0">
							<span
								class="inline-block flex-shrink-0 rounded-full border border-gold/50 bg-gold/15 px-2.5 py-0.5 text-xs font-semibold text-gold"
							>
								{slide.tag()}
							</span>
							<p class="truncate text-xs sm:text-sm font-display font-medium text-paper">
								{slide.title()}
							</p>
						</div>
						<span class="flex-shrink-0 font-mono text-[11px] text-paper-dim">
							{((idx - 1 + slides.length) % slides.length) + 1} / {slides.length}
						</span>
					</div>
				</div>
			{/each}
		</div>
	</div>

	<!-- Controls & Navigation Deck -->
	<div class="mt-4 flex flex-col items-center gap-3 w-full">
		<!-- Capsule Player Console (REW, PLAY/PAUSE with LED, FWD) -->
		<div
			class="player-deck inline-flex items-center rounded-full border border-ink-line bg-ink px-1.5 py-1 shadow-md"
			role="toolbar"
			aria-label="Player controls"
		>
			<!-- REW button -->
			<button
				type="button"
				class="deck-btn"
				onclick={handleManualPrev}
				aria-label={m.home_carousel_prev()}
				title={m.home_carousel_prev()}
			>
				<svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
					<polygon points="11 19 2 12 11 5 11 19" fill="currentColor" />
					<polygon points="22 19 13 12 22 5 22 19" fill="currentColor" />
				</svg>
				<span class="text-xs font-display font-semibold tracking-wider">REW</span>
			</button>

			<div class="deck-divider" aria-hidden="true"></div>

			<!-- PLAY / PAUSE button with glowing status LED -->
			<button
				type="button"
				class="deck-btn playpause-btn"
				class:is-playing={isPlaying}
				onclick={togglePlayPause}
				aria-label={isPlaying ? m.home_carousel_pause() : m.home_carousel_play()}
				title={isPlaying ? m.home_carousel_pause() : m.home_carousel_play()}
			>
				<span class="status-led" class:led-on={isPlaying} aria-hidden="true"></span>
				{#if isPlaying}
					<svg class="h-4 w-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
						<rect x="6" y="4" width="4" height="16" rx="1.5" />
						<rect x="14" y="4" width="4" height="16" rx="1.5" />
					</svg>
					<span class="text-xs font-display font-semibold tracking-wider">PAUSE</span>
				{:else}
					<svg class="h-4 w-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
						<polygon points="6 4 20 12 6 20 6 4" />
					</svg>
					<span class="text-xs font-display font-semibold tracking-wider">PLAY</span>
				{/if}
			</button>

			<div class="deck-divider" aria-hidden="true"></div>

			<!-- FWD button -->
			<button
				type="button"
				class="deck-btn"
				onclick={handleManualNext}
				aria-label={m.home_carousel_next()}
				title={m.home_carousel_next()}
			>
				<span class="text-xs font-display font-semibold tracking-wider">FWD</span>
				<svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
					<polygon points="13 19 22 12 13 5 13 19" fill="currentColor" />
					<polygon points="2 19 11 12 2 5 2 19" fill="currentColor" />
				</svg>
			</button>
		</div>

		<!-- Step Dots Indicators with progress fill -->
		<div
			class="flex items-center gap-2"
			role="tablist"
			aria-label={m.home_carousel_aria()}
		>
			{#each slides as slide, idx (slide.id)}
				<button
					type="button"
					class="dot-pill relative overflow-hidden rounded-full border border-ink-line bg-ink px-3 py-1 text-xs font-display transition-colors"
					class:active={activeSlideIndex === idx}
					onclick={() => selectSlide(idx)}
					role="tab"
					aria-selected={activeSlideIndex === idx}
					aria-label={m.home_carousel_step_aria({ num: idx + 1, total: slides.length, title: slide.title() })}
					title="{slide.title()} ({idx + 1} / {slides.length})"
				>
					<span class="font-semibold text-paper-dim hover:text-paper" class:text-gold={activeSlideIndex === idx}>
						{idx + 1}
					</span>
					{#if activeSlideIndex === idx && isPlaying && !isHovered}
						<span class="progress-bar" aria-hidden="true"></span>
					{/if}
				</button>
			{/each}
		</div>
	</div>
</div>

<style>
	.carousel-track {
		display: flex;
		width: 100%;
		transition: transform 700ms cubic-bezier(0.25, 1, 0.5, 1);
		will-change: transform;
	}

	.carousel-track.no-transition {
		transition: none !important;
	}

	.carousel-slide {
		width: 100%;
	}

	/* Tap / click pop feedback */
	.tap-feedback-badge {
		position: absolute;
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
		width: 58px;
		height: 58px;
		border-radius: 50%;
		background: rgba(15, 23, 42, 0.75);
		backdrop-filter: blur(8px);
		border: 1px solid rgba(255, 255, 255, 0.2);
		color: #ffffff;
		display: flex;
		align-items: center;
		justify-content: center;
		pointer-events: none;
		z-index: 30;
		box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
		animation: tap-pop 650ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
	}

	@keyframes tap-pop {
		0% {
			opacity: 0;
			transform: translate(-50%, -50%) scale(0.6);
		}
		35% {
			opacity: 1;
			transform: translate(-50%, -50%) scale(1.08);
		}
		70% {
			opacity: 0.95;
			transform: translate(-50%, -50%) scale(1);
		}
		100% {
			opacity: 0;
			transform: translate(-50%, -50%) scale(1.2);
		}
	}

	/* Deck buttons & dividers */
	.deck-btn {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
		padding: 0.4rem 0.85rem;
		border-radius: 9999px;
		color: var(--color-paper-dim);
		background: transparent;
		transition: all 120ms ease;
		cursor: pointer;
	}

	.deck-btn:hover {
		color: var(--color-paper);
		background: var(--color-ink-soft);
	}

	.deck-btn:active {
		transform: scale(0.96);
	}

	.playpause-btn.is-playing {
		color: var(--color-gold);
		background: rgba(229, 169, 59, 0.1);
	}

	.deck-divider {
		width: 1px;
		height: 16px;
		background: var(--color-ink-line);
		margin: 0 0.1rem;
	}

	/* Glowing LED */
	.status-led {
		width: 6px;
		height: 6px;
		border-radius: 50%;
		background: #64748b;
		transition: all 200ms ease;
	}

	.status-led.led-on {
		background: var(--color-teal);
		box-shadow: 0 0 8px var(--color-teal);
	}

	/* Indicator dots */
	.dot-pill.active {
		border-color: var(--color-gold);
		background: rgba(229, 169, 59, 0.15);
	}

	.progress-bar {
		position: absolute;
		bottom: 0;
		left: 0;
		height: 2px;
		background: var(--color-gold);
		animation: progress-fill 5s linear forwards;
	}

	@keyframes progress-fill {
		from {
			width: 0%;
		}
		to {
			width: 100%;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.carousel-track {
			transition: none !important;
		}
		.progress-bar {
			animation: none;
		}
		.tap-feedback-badge {
			animation: none;
		}
	}
</style>
