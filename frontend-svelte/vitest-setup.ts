import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/svelte';
import { afterEach } from 'vitest';

// @testing-library/svelte doesn't auto-register cleanup the way its
// React counterpart does — without this, each render() in a test file
// stacks up in the same jsdom document instead of unmounting between tests.
afterEach(() => cleanup());
