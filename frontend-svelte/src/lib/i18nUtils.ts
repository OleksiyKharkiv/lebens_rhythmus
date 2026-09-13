import { getLocale } from '$lib/paraglide/runtime';

/**
 * Extracts a localized field from an object with multi-language fields (e.g. titleDe, titleEn, titleUa).
 * Falls back to the German version (e.g. titleDe) if the requested locale's field is empty or missing.
 *
 * @param item An object containing multilingual fields
 * @param prefix The field name prefix (e.g. 'title', 'description', 'formatDisclaimer')
 * @returns The localized string or fallback to German or empty string
 */
export function getLocalizedField<T extends Record<string, any>>(
	item: T | null | undefined,
	prefix: string
): string {
	if (!item) return '';
	const locale = getLocale();
	const suffix = locale === 'uk' ? 'Ua' : locale === 'en' ? 'En' : 'De';
	const targetKey = `${prefix}${suffix}`;
	const targetVal = item[targetKey];
	if (typeof targetVal === 'string' && targetVal.trim().length > 0) {
		return targetVal;
	}
	const fallbackVal = item[`${prefix}De`];
	return typeof fallbackVal === 'string' ? fallbackVal : '';
}
