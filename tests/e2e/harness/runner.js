/**
 * OmniTune Master E2E Test Suite Runner
 *
 * Discovers, orchestrates, executes, and validates all E2E test tiers.
 * Verifies coverage thresholds, produces formatted reports, and sets exit code.
 */

const path = require('path');

// ANSI Color Helpers
const colors = {
  reset: '\x1b[0m',
  bold: '\x1b[1m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
  gray: '\x1b[90m',
  bgGreen: '\x1b[42m\x1b[30m',
  bgRed: '\x1b[41m\x1b[37m'
};

class TestRegistry {
  constructor() {
    this.tests = [];
  }

  /**
   * Registers a test case.
   * @param {Object} def - { tier: 1|2|3|4, featureId: string, name: string, fn: Function }
   */
  register(def) {
    this.tests.push(def);
  }
}

const registry = new TestRegistry();

// Test registration helper functions exposed globally to test modules
function registerTest(tier, featureId, name, fn) {
  registry.register({ tier, featureId, name, fn });
}

async function runRunner() {
  console.log(`${colors.bold}${colors.cyan}========================================================================${colors.reset}`);
  console.log(`${colors.bold}${colors.cyan}             OMNITUNE iOS KMP COMPREHENSIVE E2E TEST RUNNER            ${colors.reset}`);
  console.log(`${colors.bold}${colors.cyan}========================================================================${colors.reset}\n`);

  // Parse command line arguments
  const args = process.argv.slice(2);
  let tierFilter = null;
  const tierArgIdx = args.indexOf('--tier');
  if (tierArgIdx !== -1 && args[tierArgIdx + 1]) {
    tierFilter = parseInt(args[tierArgIdx + 1], 10);
  }

  // Load test suites
  const suiteDir = path.resolve(__dirname, '..');
  
  // Expose test registration to suites
  global.__registerTest = registerTest;

  require(path.join(suiteDir, 'tier1_features.test.js'));
  require(path.join(suiteDir, 'tier2_boundaries.test.js'));
  require(path.join(suiteDir, 'tier3_interactions.test.js'));
  require(path.join(suiteDir, 'tier4_realworld.test.js'));

  let testsToRun = registry.tests;
  if (tierFilter !== null) {
    testsToRun = testsToRun.filter(t => t.tier === tierFilter);
    console.log(`${colors.yellow}Filter active: Running Tier ${tierFilter} tests only (${testsToRun.length} tests)${colors.reset}\n`);
  } else {
    console.log(`Discovered ${colors.bold}${testsToRun.length}${colors.reset} total test cases across Tiers 1-4.\n`);
  }

  let passed = 0;
  let failed = 0;
  const failures = [];
  const tierCounts = { 1: { pass: 0, fail: 0 }, 2: { pass: 0, fail: 0 }, 3: { pass: 0, fail: 0 }, 4: { pass: 0, fail: 0 } };
  const featureCoverageTier1 = {};
  const featureCoverageTier2 = {};

  for (let i = 1; i <= 24; i++) {
    const fId = `F${String(i).padStart(2, '0')}`;
    featureCoverageTier1[fId] = 0;
    featureCoverageTier2[fId] = 0;
  }

  const startTime = Date.now();
  let currentTier = null;

  for (const test of testsToRun) {
    if (test.tier !== currentTier) {
      currentTier = test.tier;
      console.log(`\n${colors.bold}${colors.blue}--- TIER ${currentTier} EXECUTION ---${colors.reset}`);
    }

    const testStart = Date.now();
    try {
      if (test.fn.constructor.name === 'AsyncFunction') {
        await test.fn();
      } else {
        test.fn();
      }
      const durationMs = Date.now() - testStart;
      passed++;
      tierCounts[test.tier].pass++;

      if (test.tier === 1 && test.featureId) {
        featureCoverageTier1[test.featureId] = (featureCoverageTier1[test.featureId] || 0) + 1;
      }
      if (test.tier === 2 && test.featureId) {
        featureCoverageTier2[test.featureId] = (featureCoverageTier2[test.featureId] || 0) + 1;
      }

      console.log(`  ${colors.green}[PASS]${colors.reset} [Tier ${test.tier}][${test.featureId}] ${test.name} ${colors.gray}(${durationMs}ms)${colors.reset}`);
    } catch (err) {
      const durationMs = Date.now() - testStart;
      failed++;
      tierCounts[test.tier].fail++;
      failures.push({ test, err, durationMs });
      console.log(`  ${colors.red}[FAIL]${colors.reset} [Tier ${test.tier}][${test.featureId}] ${test.name} ${colors.gray}(${durationMs}ms)${colors.reset}`);
      console.log(`         ${colors.red}Error: ${err.message}${colors.reset}`);
    }
  }

  const totalDuration = Date.now() - startTime;

  // Print Summary
  console.log(`\n${colors.bold}${colors.cyan}========================================================================${colors.reset}`);
  console.log(`${colors.bold}${colors.cyan}                         TEST EXECUTION SUMMARY                         ${colors.reset}`);
  console.log(`${colors.bold}${colors.cyan}========================================================================${colors.reset}`);

  console.log(`Total Executed: ${testsToRun.length}`);
  console.log(`Passed:         ${colors.green}${passed}${colors.reset}`);
  console.log(`Failed:         ${failed > 0 ? colors.red + failed + colors.reset : colors.green + '0' + colors.reset}`);
  console.log(`Total Time:     ${totalDuration}ms\n`);

  console.log(`${colors.bold}Tier Breakdown:${colors.reset}`);
  for (let t = 1; t <= 4; t++) {
    const p = tierCounts[t].pass;
    const f = tierCounts[t].fail;
    const total = p + f;
    const status = f === 0 ? `${colors.green}ALL PASS${colors.reset}` : `${colors.red}${f} FAILURES${colors.reset}`;
    console.log(`  Tier ${t}: ${total} tests (${p} passed, ${f} failed) -> ${status}`);
  }

  // Feature Coverage Verification (Tier 1 & Tier 2 Thresholds)
  if (tierFilter === null || tierFilter === 1 || tierFilter === 2) {
    console.log(`\n${colors.bold}Feature Inventory Coverage Check (All 24 Features):${colors.reset}`);
    let allThresholdsMet = true;
    for (let i = 1; i <= 24; i++) {
      const fId = `F${String(i).padStart(2, '0')}`;
      const c1 = featureCoverageTier1[fId] || 0;
      const c2 = featureCoverageTier2[fId] || 0;
      const t1Met = (tierFilter === 2) ? true : (c1 >= 5);
      const t2Met = (tierFilter === 1) ? true : (c2 >= 5);
      const ok = t1Met && t2Met;
      if (!ok) allThresholdsMet = false;
      const statusIcon = ok ? `${colors.green}[OK]${colors.reset}` : `${colors.red}[UNDER THRESHOLD]${colors.reset}`;
      console.log(`  ${fId}: Tier 1 = ${c1}/5, Tier 2 = ${c2}/5  ${statusIcon}`);
    }

    if (!allThresholdsMet) {
      console.error(`\n${colors.red}${colors.bold}ERROR: Feature coverage threshold not met (requires >=5 tests/feature in Tier 1 and Tier 2)!${colors.reset}`);
      process.exit(1);
    }
  }

  if (failures.length > 0) {
    console.log(`\n${colors.bold}${colors.red}============================ FAILURES ==================================${colors.reset}`);
    failures.forEach((f, idx) => {
      console.log(`\n#${idx + 1} [Tier ${f.test.tier}][${f.test.featureId}] ${f.test.name}`);
      console.log(`Message: ${colors.red}${f.err.message}${colors.reset}`);
      if (f.err.stack) {
        console.log(`${colors.gray}${f.err.stack}${colors.reset}`);
      }
    });
    console.log(`\n${colors.bgRed} TEST RUN FAILED: ${failed} test(s) failed. ${colors.reset}\n`);
    process.exit(1);
  }

  console.log(`\n${colors.bgGreen} TEST RUN SUCCESSFUL: 100% PASS (${passed}/${testsToRun.length}) ${colors.reset}\n`);
  process.exit(0);
}

if (require.main === module) {
  runRunner().catch(err => {
    console.error('Fatal runner error:', err);
    process.exit(1);
  });
}

module.exports = { runRunner, registerTest, registry };
