import { describe, it, expect, vi } from 'vitest';
import { getLocalizedField } from './i18nUtils';
import * as runtime from '$lib/paraglide/runtime';

describe('getLocalizedField', () => {
	it('returns German field when locale is de', () => {
		vi.spyOn(runtime, 'getLocale').mockReturnValue('de');
		const item = { titleDe: 'Titel DE', titleEn: 'Title EN', titleUa: 'Назва UA' };
		expect(getLocalizedField(item, 'title')).toBe('Titel DE');
	});

	it('returns English field when locale is en', () => {
		vi.spyOn(runtime, 'getLocale').mockReturnValue('en');
		const item = { titleDe: 'Titel DE', titleEn: 'Title EN', titleUa: 'Назва UA' };
		expect(getLocalizedField(item, 'title')).toBe('Title EN');
	});

	it('returns Ukrainian field when locale is uk', () => {
		vi.spyOn(runtime, 'getLocale').mockReturnValue('uk');
		const item = { titleDe: 'Titel DE', titleEn: 'Title EN', titleUa: 'Назва UA' };
		expect(getLocalizedField(item, 'title')).toBe('Назва UA');
	});

	it('falls back to German when localized field is missing or empty', () => {
		vi.spyOn(runtime, 'getLocale').mockReturnValue('en');
		const item = { titleDe: 'Titel DE', titleEn: '', titleUa: 'Назва UA' };
		expect(getLocalizedField(item, 'title')).toBe('Titel DE');
	});

	it('returns empty string when item is null or undefined', () => {
		expect(getLocalizedField(null, 'title')).toBe('');
		expect(getLocalizedField(undefined, 'title')).toBe('');
	});
});
