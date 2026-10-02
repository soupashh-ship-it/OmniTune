/**
 * OmniTune E2E Test Suite - Strict Behavioral Assertion Primitives
 *
 * Provides deterministic assertions with clear diagnostic diffs and failure reporting.
 */

class AssertionError extends Error {
  constructor(message, expected, actual) {
    super(message);
    this.name = 'AssertionError';
    this.expected = expected;
    this.actual = actual;
  }
}

function assertEqual(actual, expected, message = '') {
  if (actual !== expected) {
    const msg = message ? `${message}: ` : '';
    throw new AssertionError(
      `${msg}Expected ${JSON.stringify(expected)}, but got ${JSON.stringify(actual)}`,
      expected,
      actual
    );
  }
}

function assertNotEqual(actual, expected, message = '') {
  if (actual === expected) {
    const msg = message ? `${message}: ` : '';
    throw new AssertionError(
      `${msg}Expected value NOT to equal ${JSON.stringify(expected)}`,
      `NOT ${JSON.stringify(expected)}`,
      actual
    );
  }
}

function assertTrue(value, message = '') {
  if (value !== true) {
    const msg = message ? `${message}: ` : '';
    throw new AssertionError(`${msg}Expected true, but got ${JSON.stringify(value)}`, true, value);
  }
}

function assertFalse(value, message = '') {
  if (value !== false) {
    const msg = message ? `${message}: ` : '';
    throw new AssertionError(`${msg}Expected false, but got ${JSON.stringify(value)}`, false, value);
  }
}

function assertNull(value, message = '') {
  if (value !== null && value !== undefined) {
    const msg = message ? `${message}: ` : '';
    throw new AssertionError(`${msg}Expected null/undefined, but got ${JSON.stringify(value)}`, null, value);
  }
}

function assertNotNull(value, message = '') {
  if (value === null || value === undefined) {
    const msg = message ? `${message}: ` : '';
    throw new AssertionError(`${msg}Expected non-null value, but got ${value}`, 'non-null', value);
  }
}

function assertDeepEqual(actual, expected, message = '') {
  const actualStr = JSON.stringify(actual);
  const expectedStr = JSON.stringify(expected);
  if (actualStr !== expectedStr) {
    const msg = message ? `${message}: ` : '';
    throw new AssertionError(
      `${msg}Deep equality failure.\nExpected: ${expectedStr}\nActual:   ${actualStr}`,
      expected,
      actual
    );
  }
}

function assertIncludes(haystack, needle, message = '') {
  const msg = message ? `${message}: ` : '';
  if (typeof haystack === 'string') {
    if (!haystack.includes(needle)) {
      throw new AssertionError(
        `${msg}String does not contain substring.\nHaystack: ${haystack}\nNeedle: ${needle}`,
        needle,
        haystack
      );
    }
  } else if (Array.isArray(haystack)) {
    if (!haystack.includes(needle)) {
      throw new AssertionError(
        `${msg}Array does not contain element.\nArray: ${JSON.stringify(haystack)}\nElement: ${JSON.stringify(needle)}`,
        needle,
        haystack
      );
    }
  } else {
    throw new AssertionError(`${msg}Unsupported haystack type: ${typeof haystack}`);
  }
}

function assertInRange(val, min, max, message = '') {
  const msg = message ? `${message}: ` : '';
  if (val < min || val > max) {
    throw new AssertionError(
      `${msg}Value ${val} is outside expected range [${min}, ${max}]`,
      `[${min}, ${max}]`,
      val
    );
  }
}

function assertMatches(val, regex, message = '') {
  const msg = message ? `${message}: ` : '';
  if (!regex.test(String(val))) {
    throw new AssertionError(
      `${msg}Value "${val}" does not match pattern ${regex.toString()}`,
      regex.toString(),
      val
    );
  }
}

function assertThrows(fn, expectedRegex = null, message = '') {
  const msg = message ? `${message}: ` : '';
  let caught = false;
  let caughtError = null;
  try {
    fn();
  } catch (err) {
    caught = true;
    caughtError = err;
  }
  if (!caught) {
    throw new AssertionError(`${msg}Expected function to throw, but it succeeded without error.`);
  }
  if (expectedRegex && !expectedRegex.test(caughtError.message)) {
    throw new AssertionError(
      `${msg}Thrown error message "${caughtError.message}" does not match ${expectedRegex.toString()}`,
      expectedRegex.toString(),
      caughtError.message
    );
  }
}

async function assertThrowsAsync(asyncFn, expectedRegex = null, message = '') {
  const msg = message ? `${message}: ` : '';
  let caught = false;
  let caughtError = null;
  try {
    await asyncFn();
  } catch (err) {
    caught = true;
    caughtError = err;
  }
  if (!caught) {
    throw new AssertionError(`${msg}Expected async function to throw, but it succeeded without error.`);
  }
  if (expectedRegex && !expectedRegex.test(caughtError.message)) {
    throw new AssertionError(
      `${msg}Thrown error message "${caughtError.message}" does not match ${expectedRegex.toString()}`,
      expectedRegex.toString(),
      caughtError.message
    );
  }
}

module.exports = {
  AssertionError,
  assertEqual,
  assertNotEqual,
  assertTrue,
  assertFalse,
  assertNull,
  assertNotNull,
  assertDeepEqual,
  assertIncludes,
  assertInRange,
  assertMatches,
  assertThrows,
  assertThrowsAsync,
};
