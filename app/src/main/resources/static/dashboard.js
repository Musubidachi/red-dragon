const $ = (sel) => document.querySelector(sel);
  const candidatesEl = $('#candidates');
  const candidateDetailEl = $('#candidate-detail');
  const calibrationReportEl = $('#calibration-report');
  const lookbackEl = $('#lookback');
  const refreshBtn = $('#refresh');
  const outcomesLoadMoreBtn = $('#outcomes-load-more');
  const apiKeyEl = $('#api-key');
  const tickerAnalysisForm = $('#ticker-analysis-form');
  const tickerAnalysisResultEl = $('#ticker-analysis-result');
  const analysisProfileEl = $('#analysis-profile');
  const backtestForm = $('#backtest-form');
  const backtestRunsEl = $('#backtest-runs');
  const backtestResultEl = $('#backtest-result');
  const backtestFramesEl = $('#backtest-frames');
  const tradeImportForm = $('#trade-import-form');
  const tradeImportCsvEl = $('#trade-import-csv');
  const tradeImportsEl = $('#trade-imports');
  const tradeHistoryEl = $('#trade-history');
  const tradeFilterTickerEl = $('#trade-filter-ticker');
  const secWatchForm = $('#sec-watch-form');
  const secWatchProfileEl = $('#sec-watch-profile');
  const secWatchResultEl = $('#sec-watch-result');
  const marketStateForm = $('#market-state-form');
  const marketStateBarsEl = $('#market-state-bars');
  const marketStateResultEl = $('#market-state-result');
  const defaultApiKey = 'dev-red-dragon-api-key';
  let activeVerdict = 'PASS,WATCH';
  let outcomesOffset = 0;
  let outcomesLimit = 25;
  let outcomesHasMore = true;
  let apiKey = localStorage.getItem('redDragonApiKey') || defaultApiKey;
  let selectedCandidateId = null;
  let selectedCandidateCard = null;
  let candidateEventSource = null;

  apiKeyEl.value = apiKey;
  apiKeyEl.addEventListener('change', () => {
    apiKey = apiKeyEl.value.trim() || defaultApiKey;
    localStorage.setItem('redDragonApiKey', apiKey);
    closeCandidateStream();
    refreshAll();
  });

  document.querySelectorAll('.pill[data-verdict]').forEach((btn) => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.pill[data-verdict]').forEach((item) => item.classList.remove('active'));
      btn.classList.add('active');
      activeVerdict = btn.dataset.verdict;
      loadCandidates();
      connectCandidateStream();
    });
  });

  refreshBtn.addEventListener('click', refreshAll);
  lookbackEl.addEventListener('change', () => {
    closeCandidateStream();
    refreshAll();
  });
  outcomesLoadMoreBtn.addEventListener('click', () => loadOutcomesJournal(false));
  $('#outcomes-export').addEventListener('click', downloadOutcomesCsv);
  tickerAnalysisForm.addEventListener('submit', runTickerAnalysis);
  backtestForm.addEventListener('submit', runBacktest);
  $('#backtest-load-run').addEventListener('click', loadSelectedBacktestRun);
  $('#backtest-sample').addEventListener('click', fillBacktestSample);
  tradeImportForm.addEventListener('submit', importTradeHistory);
  $('#trade-import-sample').addEventListener('click', fillTradeHistorySample);
  $('#trade-refresh').addEventListener('click', loadTradeHistory);
  secWatchForm.addEventListener('submit', runSecWatchList);
  marketStateForm.addEventListener('submit', classifyMarketState);
  $('#market-state-sample').addEventListener('click', fillMarketStateSample);

  function fmtScore(value) {
    return Number(value || 0).toFixed(2);
  }

  function fmtPct(value) {
    return (Number(value || 0) * 100).toFixed(1) + '%';
  }

  function fmtInt(value) {
    return new Intl.NumberFormat().format(Number(value || 0));
  }

  function fmtMoney(value) {
    if (value == null || value === '') return '-';
    const number = Number(value);
    if (!Number.isFinite(number) || number === 0) return '-';
    return (number < 0 ? '-$' : '$') + Math.abs(number).toFixed(2);
  }

  function fmtCompact(value) {
    return new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 }).format(Number(value || 0));
  }

  function fmtDate(value) {
    if (!value) return '-';
    return new Date(value).toLocaleString();
  }

  function fmtPlain(value) {
    return String(value == null || value === '' ? '-' : value).replace(/_/g, ' ');
  }

  function driftClass(value) {
    const drift = String(value || '').toUpperCase();
    if (drift === 'STABLE') return 'pass';
    if (drift === 'MINOR_DRIFT') return 'watch';
    return drift ? 'reject' : '';
  }

  function normalizeMarketBar(row) {
    if (!row) return null;
    return {
      date: row.date || row.barDate,
      open: Number(row.open == null ? row.openPrice : row.open),
      high: Number(row.high == null ? row.highPrice : row.high),
      low: Number(row.low == null ? row.lowPrice : row.low),
      close: Number(row.close == null ? row.closePrice : row.close),
      volume: Number(row.volume || 0)
    };
  }

  function escapeHtml(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, (ch) => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', '\'': '&#39;' }[ch]
    ));
  }

  function authHeaders(extra) {
    return { ...(extra || {}), Authorization: 'Bearer ' + apiKey };
  }

  function verdictCount(map, key) {
    if (!map) return 0;
    if (Object.prototype.hasOwnProperty.call(map, key)) return Number(map[key] || 0);
    const alt = Object.keys(map).find((item) => item.toUpperCase() === key);
    return alt ? Number(map[alt] || 0) : 0;
  }

  function topTier(tierMap) {
    if (!tierMap) return { name: '-', count: 0 };
    return Object.entries(tierMap).sort((a, b) => Number(b[1]) - Number(a[1]))[0]
      ? { name: Object.entries(tierMap).sort((a, b) => Number(b[1]) - Number(a[1]))[0][0], count: Number(Object.entries(tierMap).sort((a, b) => Number(b[1]) - Number(a[1]))[0][1]) }
      : { name: '-', count: 0 };
  }

  async function fetchJson(url) {
    const res = await fetch(url, { headers: authHeaders() });
    if (!res.ok) throw new Error(res.status + ' ' + res.statusText);
    return res.json();
  }

  async function sendJson(url, method, body) {
    const res = await fetch(url, {
      method,
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(body)
    });
    if (!res.ok) throw new Error(res.status + ' ' + res.statusText);
    return res.json();
  }

  function closeCandidateStream() {
    if (candidateEventSource) {
      candidateEventSource.close();
      candidateEventSource = null;
    }
  }

  function connectCandidateStream() {
    if (!window.EventSource) return;
    closeCandidateStream();
    const lookback = Math.max(1, parseInt(lookbackEl.value || '24', 10));
    const params = new URLSearchParams({
      verdicts: activeVerdict,
      lookbackHours: String(lookback),
      apiKey
    });
    candidateEventSource = new EventSource('/api/review/candidates/stream?' + params.toString());
    candidateEventSource.addEventListener('candidates', (event) => {
      renderCandidateList(JSON.parse(event.data || '[]'));
    });
    candidateEventSource.onerror = () => {
      loadCandidates();
    };
  }

  async function loadPipelineStatus() {
    try {
      const status = await fetchJson('/api/pipeline/status');
      $('#pipeline-state').textContent = status.status || 'UP';
      $('#pipeline-candidates').textContent = fmtInt(status.candidateCount);
      $('#pipeline-verdicts').textContent = fmtInt(status.verdictCount);
      $('#pipeline-checked-at').textContent = fmtDate(status.checkedAt);
    } catch (err) {
      $('#pipeline-state').textContent = 'ERROR';
      $('#pipeline-checked-at').textContent = 'Unavailable';
    }
  }

  async function loadStats() {
    const lookback = Math.max(1, parseInt(lookbackEl.value || '24', 10));
    try {
      const [summary, verdictStats] = await Promise.all([
        fetchJson('/api/calibration/summary?limit=100'),
        fetchJson('/api/verdicts/stats?lookbackHours=' + lookback)
      ]);

      $('#stat-samples').textContent = fmtInt(summary.sampleSize);
      $('#stat-winrate').textContent = fmtPct(summary.winRate);
      $('#stat-avgreturn').textContent = fmtPct(summary.averageReturn);
      $('#stat-avgdrawdown').textContent = fmtPct(summary.averageDrawdown);
      $('#stat-pass').textContent = fmtInt(verdictCount(verdictStats.byVerdict, 'PASS'));
      $('#stat-watch').textContent = fmtInt(verdictCount(verdictStats.byVerdict, 'WATCH'));
      $('#stat-reject').textContent = fmtInt(verdictCount(verdictStats.byVerdict, 'REJECT'));

      const tier = topTier(verdictStats.byDeploymentTier);
      $('#stat-tier').textContent = tier.name;
      $('#stat-tier-count').textContent = tier.count > 0 ? fmtInt(tier.count) + ' recent hits' : 'No actionable tier data';
    } catch (err) {
      ['stat-samples', 'stat-winrate', 'stat-avgreturn', 'stat-avgdrawdown', 'stat-pass', 'stat-watch', 'stat-reject', 'stat-tier']
        .forEach((id) => { $('#' + id).textContent = '-'; });
      $('#stat-tier-count').textContent = 'Unavailable';
    }
  }

  async function loadCandidates() {
    candidatesEl.innerHTML = '<div class="empty">Loading...</div>';
    const lookback = Math.max(1, parseInt(lookbackEl.value || '24', 10));
    try {
      const items = await fetchJson('/api/review/candidates?verdicts=' + encodeURIComponent(activeVerdict) + '&lookbackHours=' + lookback);
      renderCandidateList(items);
    } catch (err) {
      candidatesEl.innerHTML = '<div class="empty error">Failed to load candidates: ' + escapeHtml(err.message) + '</div>';
    }
  }

  function renderCandidateList(items) {
    if (!items || items.length === 0) {
      candidatesEl.innerHTML = '<div class="empty">No candidates in this window.</div>';
      return;
    }
    const grid = document.createElement('div');
    grid.className = 'candidate-grid';
    items.forEach((item) => grid.appendChild(candidateCard(item)));
    candidatesEl.innerHTML = '';
    candidatesEl.appendChild(grid);
    loadCardContexts(items, grid);
    if (selectedCandidateId && !items.some((item) => item.candidateId === selectedCandidateId)) {
      selectedCandidateId = null;
      candidateDetailEl.innerHTML = '';
    }
  }

  async function loadCalibrationReport() {
    try {
      const report = await fetchJson('/api/calibration');
      const findings = report.findings || [];
      const recommendations = report.recommendations || [];
      calibrationReportEl.innerHTML = `
        <div class="analysis-headline">
          <span class="chip ${driftClass(report.driftLevel)}">${escapeHtml(fmtPlain(report.driftLevel))}</span>
        </div>
        <div class="metric-grid">
          ${metric('Historical win rate', fmtPct(report.historicalWinRate))}
          ${metric('Average return', fmtPct(report.averageReturn))}
          ${metric('Average drawdown', fmtPct(report.averageDrawdown))}
          ${metric('Findings', fmtInt(findings.length))}
        </div>
        <div class="split-grid">
          ${listBlock('Findings', findings)}
          ${listBlock('Recommendations', recommendations)}
        </div>
      `;
    } catch (err) {
      calibrationReportEl.innerHTML = '<div class="empty error">Calibration report unavailable: '
        + escapeHtml(err.message) + '</div>';
    }
  }

  async function loadValidationProfiles() {
    try {
      const profiles = await fetchJson('/api/validation/profiles');
      const options = profiles.map((profile) => (
        '<option value="' + escapeHtml(profile.name) + '">' + escapeHtml(profile.displayName || profile.name) + '</option>'
      )).join('');
      analysisProfileEl.innerHTML = options;
      secWatchProfileEl.innerHTML = options;
      const standard = Array.from(analysisProfileEl.options).find((option) => option.value === 'STANDARD');
      if (standard) {
        analysisProfileEl.value = 'STANDARD';
      }
      const secStandard = Array.from(secWatchProfileEl.options).find((option) => option.value === 'STANDARD');
      if (secStandard) {
        secWatchProfileEl.value = 'STANDARD';
      }
    } catch (err) {
      analysisProfileEl.innerHTML = '<option value="STANDARD">Standard</option>';
      secWatchProfileEl.innerHTML = '<option value="STANDARD">Standard</option>';
    }
  }

  function fillBacktestSample() {
    backtestFramesEl.value = JSON.stringify([
      {
        symbol: 'ASTS',
        companyName: 'AST SpaceMobile',
        catalystType: 'MANUAL_THESIS',
        headline: 'Historical setup sample',
        summary: 'Dashboard-entered backtest frame.',
        structuralRealityScore: 0.80,
        materialSignificanceScore: 0.72,
        earlynessScore: 0.66,
        reflexivityPotentialScore: 0.70,
        bars: [
          { date: '2026-01-02', open: 20.00, high: 21.20, low: 19.80, close: 20.90, volume: 1000000 },
          { date: '2026-01-03', open: 20.90, high: 22.10, low: 20.70, close: 21.80, volume: 1200000 },
          { date: '2026-01-04', open: 21.80, high: 23.00, low: 21.40, close: 22.50, volume: 1300000 }
        ]
      }
    ], null, 2);
  }

  function fillTradeHistorySample() {
    tradeImportCsvEl.value = [
      'timestamp,ticker,side,quantity,price,realizedPnL,account',
      '2026-05-27T12:00:00Z,ASTS,BUY,10,22.50,,main',
      '2026-05-28T15:30:00Z,ASTS,SELL,5,24.10,8.00,main'
    ].join('\n');
  }

  function fillMarketStateSample() {
    marketStateBarsEl.value = JSON.stringify([
      { symbol: 'SPY', startTime: '2026-05-28T13:30:00Z', open: 525.10, high: 526.20, low: 524.80, close: 525.90, volume: 8200000, vwap: 525.50 },
      { symbol: 'SPY', startTime: '2026-05-28T13:35:00Z', open: 525.90, high: 527.10, low: 525.70, close: 526.80, volume: 7600000, vwap: 526.35 },
      { symbol: 'SPY', startTime: '2026-05-28T13:40:00Z', open: 526.80, high: 527.60, low: 526.20, close: 527.30, volume: 6900000, vwap: 526.90 },
      { symbol: 'SPY', startTime: '2026-05-28T13:45:00Z', open: 527.30, high: 527.90, low: 526.90, close: 527.70, volume: 6400000, vwap: 527.35 }
    ], null, 2);
  }

  async function loadBacktestRuns() {
    try {
      const runs = await fetchJson('/api/backtest/runs');
      backtestRunsEl.innerHTML = runs && runs.length
        ? runs.map((runId) => '<option value="' + escapeHtml(runId) + '">' + escapeHtml(runId) + '</option>').join('')
        : '<option value="">No runs</option>';
    } catch (err) {
      backtestRunsEl.innerHTML = '<option value="">Unavailable</option>';
    }
  }

  async function runBacktest(event) {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(backtestForm));
    $('#backtest-status').textContent = 'Running...';
    try {
      const frames = JSON.parse(data.framesJson || '[]');
      const response = await sendJson('/api/backtest', 'POST', {
        strategyName: data.strategyName,
        frames
      });
      $('#backtest-status').textContent = 'Run saved: ' + response.runId;
      renderBacktestReport(response.runId, response.report);
      await loadBacktestRuns();
      if (response.runId) {
        backtestRunsEl.value = response.runId;
      }
      await loadPerformance();
      await loadOutcomesJournal(true);
    } catch (err) {
      $('#backtest-status').textContent = 'Failed: ' + err.message;
      backtestResultEl.innerHTML = '<div class="empty error">Backtest failed: ' + escapeHtml(err.message) + '</div>';
    }
  }

  async function loadSelectedBacktestRun() {
    const runId = backtestRunsEl.value;
    if (!runId) return;
    $('#backtest-status').textContent = 'Loading run...';
    try {
      const results = await fetchJson('/api/backtest/results/' + encodeURIComponent(runId));
      $('#backtest-status').textContent = 'Loaded ' + fmtInt(results.length) + ' results';
      renderBacktestResults(runId, results);
    } catch (err) {
      $('#backtest-status').textContent = 'Failed: ' + err.message;
      backtestResultEl.innerHTML = '<div class="empty error">Run load failed: ' + escapeHtml(err.message) + '</div>';
    }
  }

  function renderBacktestReport(runId, report) {
    const metrics = report.metrics || {};
    const counts = metrics.verdictCounts || {};
    backtestResultEl.innerHTML = `
      <div class="analysis-headline">
        <strong>${escapeHtml(report.strategyName || '-')}</strong>
        <span class="chip">${escapeHtml(runId || '-')}</span>
      </div>
      <div class="metric-grid">
        ${metric('Frames', fmtInt(metrics.totalFrames))}
        ${metric('Average score', fmtScore(metrics.averageScore))}
        ${metric('PASS', fmtInt(counts.PASS))}
        ${metric('WATCH', fmtInt(counts.WATCH))}
        ${metric('REJECT', fmtInt(counts.REJECT))}
      </div>
      ${detailTable('Outcomes', ['Symbol', 'Verdict', 'Score'], (report.outcomes || []).map((outcome) => [
        outcome.candidate ? outcome.candidate.symbol : '-',
        outcome.validation ? outcome.validation.verdict : '-',
        outcome.validation ? fmtScore(outcome.validation.score) : '-'
      ]))}
    `;
  }

  function renderBacktestResults(runId, results) {
    backtestResultEl.innerHTML = `
      <div class="analysis-headline">
        <strong>Stored run</strong>
        <span class="chip">${escapeHtml(runId)}</span>
      </div>
      ${detailTable('Results', ['Symbol', 'Verdict', 'Score', 'Tested'], results.map((row) => [
        row.symbol || '-', row.verdict || '-', fmtScore(row.score), fmtDate(row.testedAt)
      ]))}
    `;
  }

  async function importTradeHistory(event) {
    event.preventDefault();
    $('#trade-import-status').textContent = 'Importing...';
    try {
      const response = await sendJson('/api/history/import', 'POST', {
        csv: tradeImportCsvEl.value
      });
      const warningCount = (response.warnings || []).length;
      $('#trade-import-status').textContent = 'Imported ' + fmtInt(response.importedRows)
        + ' of ' + fmtInt(response.totalRows) + ' rows'
        + (warningCount ? ' with ' + fmtInt(warningCount) + ' warnings' : '');
      await loadTradeHistory();
    } catch (err) {
      $('#trade-import-status').textContent = 'Import failed: ' + err.message;
    }
  }

  async function loadTradeHistory() {
    try {
      const ticker = tradeFilterTickerEl.value.trim();
      const tradesUrl = '/api/history/trades?lookbackHours=720'
        + (ticker ? '&ticker=' + encodeURIComponent(ticker) : '');
      const [imports, trades] = await Promise.all([
        fetchJson('/api/history/imports'),
        fetchJson(tradesUrl)
      ]);
      tradeImportsEl.innerHTML = imports && imports.length
        ? imports.map((row) => `
            <tr>
              <td>${fmtDate(row.importedAt)}</td>
              <td>${fmtInt(row.importedRows)} / ${fmtInt(row.totalRows)}</td>
              <td>${escapeHtml(row.warnings || '-')}</td>
            </tr>
          `).join('')
        : '<tr><td colspan="3" style="color:var(--fg-muted);">No imports.</td></tr>';
      tradeHistoryEl.innerHTML = trades && trades.length
        ? trades.slice(0, 25).map((row) => `
            <tr>
              <td>${fmtDate(row.tradeTimestamp)}</td>
              <td>${escapeHtml(row.ticker)}</td>
              <td>${escapeHtml(row.side)}</td>
              <td>${fmtInt(row.quantity)}</td>
              <td>${fmtMoney(row.price)}</td>
              <td>${row.realizedPnl == null ? '-' : fmtMoney(row.realizedPnl)}</td>
            </tr>
          `).join('')
        : '<tr><td colspan="6" style="color:var(--fg-muted);">No trades.</td></tr>';
    } catch (err) {
      tradeImportsEl.innerHTML = '<tr><td colspan="3" class="error">Unavailable</td></tr>';
      tradeHistoryEl.innerHTML = '<tr><td colspan="6" class="error">Unavailable</td></tr>';
    }
  }

  async function runSecWatchList(event) {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(secWatchForm));
    $('#sec-watch-status').textContent = 'Running...';
    secWatchResultEl.innerHTML = '';
    try {
      const results = await fetchJson('/api/pipeline/sec/watch-list?ciks='
        + encodeURIComponent(data.ciks)
        + '&lookbackDays=' + encodeURIComponent(data.lookbackDays || '30')
        + '&profile=' + encodeURIComponent(data.profile || 'STANDARD'));
      $('#sec-watch-status').textContent = 'Returned ' + fmtInt((results || []).length) + ' results';
      renderSecWatchListResults(results || []);
      await loadCandidates();
      await loadPipelineStatus();
    } catch (err) {
      $('#sec-watch-status').textContent = 'Failed: ' + err.message;
      secWatchResultEl.innerHTML = '<div class="empty error">Watch-list run failed: '
        + escapeHtml(err.message) + '</div>';
    }
  }

  function renderSecWatchListResults(results) {
    if (!results.length) {
      secWatchResultEl.innerHTML = '<div class="empty">No candidates returned.</div>';
      return;
    }
    secWatchResultEl.innerHTML = detailTable('SEC watch-list results',
      ['Symbol', 'Candidate', 'Verdict', 'Score', 'Tier', 'Duplicate'],
      results.map((row) => [
        row.candidate ? row.candidate.symbol : '-',
        row.candidate ? row.candidate.candidateId : '-',
        row.validation ? row.validation.verdict : (row.duplicate ? 'DUPLICATE' : '-'),
        row.validation ? fmtScore(row.validation.score) : '-',
        row.validation ? row.validation.deploymentTier : '-',
        row.duplicate ? 'Yes' : 'No'
      ]));
  }

  async function classifyMarketState(event) {
    event.preventDefault();
    $('#market-state-status').textContent = 'Classifying...';
    marketStateResultEl.innerHTML = '';
    try {
      const bars = JSON.parse(marketStateBarsEl.value || '[]');
      const signal = await sendJson('/api/market-state/classify', 'POST', bars);
      $('#market-state-status').textContent = 'Classified ' + fmtInt(bars.length) + ' bars';
      renderMarketStateSignal(signal);
    } catch (err) {
      $('#market-state-status').textContent = 'Failed: ' + err.message;
      marketStateResultEl.innerHTML = '<div class="empty error">Market-state classification failed: '
        + escapeHtml(err.message) + '</div>';
    }
  }

  function renderMarketStateSignal(signal) {
    const notes = signal.notes || [];
    marketStateResultEl.innerHTML = `
      <div class="analysis-headline">
        <span class="chip ${signal.deploymentSupported ? 'pass' : 'watch'}">${escapeHtml(fmtPlain(signal.regimeLabel))}</span>
        <span class="chip">${signal.deploymentSupported ? 'Deployment supported' : 'Deployment constrained'}</span>
      </div>
      <div class="metric-grid">
        ${metric('Confidence', fmtPct(signal.confidence))}
        ${metric('Restoration probability', fmtPct(signal.equilibriumRestorationProbability))}
        ${metric('Notes', fmtInt(notes.length))}
      </div>
      ${listBlock('Classifier notes', notes)}
    `;
  }

  async function runTickerAnalysis(event) {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(tickerAnalysisForm));
    const ticker = String(data.ticker || '').trim().toUpperCase();
    const lookbackDays = Math.max(5, Math.min(365, parseInt(data.lookbackDays || '60', 10)));
    const profile = data.profile || 'STANDARD';
    if (!ticker) return;

    tickerAnalysisResultEl.innerHTML = '<div class="empty">Analyzing ' + escapeHtml(ticker) + '...</div>';
    try {
      const analysis = await fetchJson('/api/analysis/' + encodeURIComponent(ticker)
        + '?lookbackDays=' + lookbackDays
        + '&profile=' + encodeURIComponent(profile));
      renderTickerAnalysis(analysis);
      loadCandidates();
    } catch (err) {
      tickerAnalysisResultEl.innerHTML = '<div class="empty error">Ticker analysis failed: '
        + escapeHtml(err.message) + '</div>';
    }
  }

  function renderTickerAnalysis(analysis) {
    const coverage = analysis.sourceCoverage || {};
    const candidate = analysis.selectedCandidate || {};
    const pipeline = analysis.pipelineResult || {};
    const validation = pipeline.validation || {};
    const research = analysis.research || {};
    const notes = analysis.notes || [];
    const secCandidates = analysis.secCandidates || [];
    tickerAnalysisResultEl.innerHTML = `
      <div class="analysis-headline">
        <span class="verdict-badge verdict-${escapeHtml(validation.verdict || 'WATCH')}">${escapeHtml(validation.verdict || 'NO VERDICT')}</span>
        <strong>${escapeHtml(analysis.ticker || candidate.symbol || '-')}</strong>
        <span class="chip">${escapeHtml(fmtPlain(analysis.analysisMode))}</span>
        <span class="chip">Confidence ${fmtPct(coverage.overallConfidence)}</span>
      </div>
      <div class="metric-grid">
        ${metric('CIK', analysis.cik)}
        ${metric('Market data', fmtPlain(coverage.marketData))}
        ${metric('SEC filings', fmtPlain(coverage.secFilings))}
        ${metric('LLM research', fmtPlain(coverage.llmResearch))}
        ${metric('Validation score', fmtScore(validation.score))}
        ${metric('Deployment tier', fmtPlain(validation.deploymentTier))}
      </div>
      <div class="split-grid">
        <div>
          <h3>Selected candidate</h3>
          <div class="detail-field"><span>${escapeHtml(fmtPlain(candidate.catalystType))}</span><strong>${escapeHtml(candidate.headline || 'No headline')}</strong></div>
          <p style="margin:0.6rem 0 0;color:var(--fg);">${escapeHtml(candidate.summary || research.summary || 'No summary available.')}</p>
        </div>
        <div>
          <h3>Research</h3>
          <div class="metric-grid">
            ${metric('Catalyst found', research.catalystFound ? 'Yes' : 'No')}
            ${metric('Structural', fmtScore(research.structuralRealityScore))}
            ${metric('Material', fmtScore(research.materialSignificanceScore))}
            ${metric('Earlyness', fmtScore(research.earlynessScore))}
            ${metric('Reflexivity', fmtScore(research.reflexivityPotentialScore))}
          </div>
        </div>
      </div>
      ${listBlock('Notes', notes)}
      ${listBlock('Missing sources', coverage.missingSources || [])}
      ${listBlock('Degraded sources', coverage.degradedSources || [])}
      ${detailTable('SEC candidates', ['Candidate', 'Catalyst', 'Observed'], secCandidates.map((item) => [
        item.candidateId || '-', fmtPlain(item.catalystType), fmtDate(item.observedAt)
      ]))}
    `;
  }

  function metric(label, value) {
    return '<div class="metric">' + escapeHtml(label) + '<strong>' + escapeHtml(value || '-') + '</strong></div>';
  }

  function listBlock(title, items) {
    if (!items || items.length === 0) {
      return '';
    }
    return '<div><h3>' + escapeHtml(title) + '</h3><ul class="reasons">'
      + items.map((item) => '<li>' + escapeHtml(item) + '</li>').join('')
      + '</ul></div>';
  }

  function candidateCard(item) {
    const el = document.createElement('div');
    el.className = 'card' + (item.candidateId === selectedCandidateId ? ' selected' : '');
    el.tabIndex = 0;
    const reasons = (item.explanations && item.explanations.length > 0 ? item.explanations : item.reasonCodes) || [];
    const topReasons = reasons.slice(0, 4);
    const more = reasons.length - topReasons.length;
    const tags = [];
    if (Number(item.noteCount || 0) > 0) {
      tags.push('<span class="card-tag">' + fmtInt(item.noteCount) + ' notes</span>');
    }
    if (item.overrideVerdict) {
      tags.push('<span class="card-tag override">Override ' + escapeHtml(item.overrideVerdict) + '</span>');
    }
    el.innerHTML = `
      <div class="card-head">
        <span class="symbol">${escapeHtml(item.symbol)}</span>
        <span class="verdict-badge verdict-${escapeHtml(item.overrideVerdict || item.verdict)}">${escapeHtml(item.overrideVerdict || item.verdict)}</span>
      </div>
      <div class="score-row">
        <div class="score-bar"><div style="width:${Math.min(100, Math.max(0, Number(item.score || 0) * 100))}%"></div></div>
        <span class="score-value">${fmtScore(item.score)}</span>
      </div>
      <div class="meta-row">
        <span>${escapeHtml(item.deploymentTier || '-')} ${item.regimeLabel ? '&middot; ' + escapeHtml(item.regimeLabel) : ''}</span>
        <span>${escapeHtml(fmtDate(item.reviewedAt))}</span>
      </div>
      <ul class="reasons">
        ${topReasons.map((reason) => '<li>' + escapeHtml(reason) + '</li>').join('')}
        ${more > 0 ? '<li class="more">+' + more + ' more...</li>' : ''}
      </ul>
      ${tags.length ? '<div class="card-tags">' + tags.join('') + '</div>' : ''}
      <div class="card-context" data-card-context="${escapeHtml(item.candidateId)}">
        <div>Symbol win<strong>-</strong></div>
        <div>Regime<strong>-</strong></div>
        <div>Quote<strong>-</strong></div>
      </div>
    `;
    el.addEventListener('click', () => selectCandidate(item.candidateId, el));
    el.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        selectCandidate(item.candidateId, el);
      }
    });
    return el;
  }

  async function loadCardContexts(items, grid) {
    let summaries = {};
    const symbols = Array.from(new Set(items.map((item) => item.symbol).filter(Boolean)));
    if (symbols.length) {
      try {
        summaries = await fetchJson('/api/calibration/summary/batch?symbols='
          + encodeURIComponent(symbols.join(',')) + '&limit=100');
      } catch (err) {
        summaries = {};
      }
    }
    await Promise.all(items.map(async (item) => {
      const context = Array.from(grid.querySelectorAll('[data-card-context]'))
        .find((node) => node.getAttribute('data-card-context') === item.candidateId);
      if (!context) return;
      try {
        const [regimes, quote] = await Promise.all([
          fetchJson('/api/regime/history?symbol=' + encodeURIComponent(item.symbol) + '&lookbackHours=48'),
          fetchJson('/api/market-data/' + encodeURIComponent(item.symbol) + '/quote')
        ]);
        const summary = summaries[item.symbol] || {};
        const latestRegime = regimes && regimes.length ? regimes[0].regimeLabel : item.regimeLabel;
        const quoteText = quote && quote.available ? fmtMoney(quote.lastPrice) : 'Unavailable';
        const quoteMeta = quote && quote.available ? 'Vol ' + fmtCompact(quote.volume) : fmtPlain((quote.notes || [])[0]);
        context.innerHTML = `
          <div>Symbol win<strong>${fmtPct(summary.winRate)} (${fmtInt(summary.sampleSize)})</strong></div>
          <div>Regime<strong>${escapeHtml(latestRegime || '-')}</strong></div>
          <div>Quote<strong>${escapeHtml(quoteText)}</strong><span>${escapeHtml(quoteMeta || '')}</span></div>
        `;
      } catch (err) {
        context.innerHTML = '<div>Symbol win<strong>-</strong></div><div>Regime<strong>-</strong></div><div>Quote<strong>-</strong></div>';
      }
    }));
  }

  async function selectCandidate(candidateId, cardEl) {
    selectedCandidateId = candidateId;
    selectedCandidateCard = cardEl;
    document.querySelectorAll('.candidate-grid .card').forEach((card) => card.classList.remove('selected'));
    cardEl.classList.add('selected');
    candidateDetailEl.innerHTML = '<div class="detail-panel">Loading detail...</div>';
    try {
      renderCandidateDetail(await fetchJson('/api/candidates/id/' + encodeURIComponent(candidateId)));
    } catch (err) {
      candidateDetailEl.innerHTML = '<div class="detail-panel error">Failed to load candidate detail: '
        + escapeHtml(err.message) + '</div>';
    }
  }

  function renderCandidateDetail(detail) {
    const verdicts = detail.verdicts || [];
    const marketSnapshots = detail.marketSnapshots || [];
    const analyticsSnapshots = detail.analyticsSnapshots || [];
    const notes = detail.notes || [];
    const overrides = detail.overrides || [];
    const latestVerdict = verdicts.length ? verdicts[0] : null;
    candidateDetailEl.innerHTML = `
      <section class="detail-panel">
        <div class="detail-head">
          <div>
            <h2 class="detail-title">${escapeHtml(detail.symbol)} - ${escapeHtml(detail.companyName || detail.candidateId)}</h2>
            <div style="color:var(--fg-muted);font-size:0.85rem;">${escapeHtml(detail.headline || '')}</div>
          </div>
          <button class="action secondary" type="button" id="detail-close">Close</button>
        </div>
        <div class="detail-grid">
          ${detailField('Candidate', detail.candidateId)}
          ${detailField('Catalyst', detail.catalystType)}
          ${detailField('Source', detail.sourceType)}
          ${detailField('Observed', fmtDate(detail.observedAt))}
        </div>
        <h3>Summary</h3>
        <p style="margin:0;color:var(--fg);">${escapeHtml(detail.summary || 'No summary available.')}</p>
        <div class="split-grid">
          ${detailTable('Verdicts', ['Verdict', 'Score', 'Tier', 'Reviewed'], verdicts.map((row) => [
            row.verdict || '-', fmtScore(row.score), row.deploymentTier || '-', fmtDate(row.reviewedAt)
          ]))}
          ${detailTable('Reasons', ['Code', 'Explanation'], verdicts.flatMap((row) => {
            const codes = row.reasonCodes || [];
            const explanations = row.explanations || [];
            return codes.map((code, index) => [code, explanations[index] || '']);
          }))}
        </div>
        <div class="split-grid">
          ${detailTable('Market snapshots', ['Observed', 'Regime', 'Score'], marketSnapshots.map((row) => [
            fmtDate(row.observedAt), row.quality || '-', fmtScore(row.liquidityScore)
          ]))}
          ${detailTable('Analytics snapshots', ['Observed', 'Regime', 'Asymmetry'], analyticsSnapshots.map((row) => [
            fmtDate(row.observedAt), row.regimeLabel || row.marketRegime || '-', fmtScore(row.asymmetryScore)
          ]))}
        </div>
        <h3>Daily price</h3>
        <div class="price-chart" id="daily-price-chart">Loading daily bars...</div>
        <div class="split-grid">
          ${detailTable('Notes', ['Created', 'Note'], notes.map((row) => [
            fmtDate(row.createdAt), row.noteText || ''
          ]))}
          ${detailTable('Overrides', ['Verdict', 'Reason', 'At'], overrides.map((row) => [
            row.overrideVerdict || row.verdict || '-', row.reason || '', fmtDate(row.overriddenAt)
          ]))}
        </div>
        <div class="split-grid">
          <div>
            <h3>Add note</h3>
            <form class="inline-form" id="note-form">
              <textarea name="note" required placeholder="Add review note"></textarea>
              <input name="author" placeholder="Author">
              <div class="button-row">
                <button class="action secondary" type="submit">Save note</button>
                <span class="inline-status" id="note-status"></span>
              </div>
            </form>
          </div>
          <div>
            <h3>Override verdict</h3>
            <form class="inline-form" id="override-form">
              <select name="verdict" ${latestVerdict && latestVerdict.verdictId ? '' : 'disabled'}>
                <option>PASS</option>
                <option>WATCH</option>
                <option>REJECT</option>
              </select>
              <textarea name="reason" placeholder="Override reason"></textarea>
              <input name="author" placeholder="Author">
              <div class="button-row">
                <button class="action secondary" type="submit" ${latestVerdict && latestVerdict.verdictId ? '' : 'disabled'}>Save override</button>
                <span class="inline-status" id="override-status">${latestVerdict && latestVerdict.verdictId ? '' : 'No verdict id available'}</span>
              </div>
            </form>
          </div>
        </div>
      </section>
    `;
    $('#detail-close').addEventListener('click', () => {
      selectedCandidateId = null;
      selectedCandidateCard = null;
      candidateDetailEl.innerHTML = '';
      document.querySelectorAll('.candidate-grid .card').forEach((card) => card.classList.remove('selected'));
    });
    $('#note-form').addEventListener('submit', (event) => saveNote(event, detail.candidateId));
    $('#override-form').addEventListener('submit', (event) => saveOverride(event, latestVerdict));
    loadDailyPriceChart(detail.symbol);
  }

  async function loadDailyPriceChart(symbol) {
    const chart = $('#daily-price-chart');
    if (!chart || !symbol) return;
    const to = new Date();
    const from = new Date();
    from.setDate(to.getDate() - 90);
    const query = '?from=' + from.toISOString().slice(0, 10) + '&to=' + to.toISOString().slice(0, 10);
    try {
      let bars = await fetchJson('/api/market-data/' + encodeURIComponent(symbol) + '/daily/stored' + query);
      if (!bars || bars.length === 0) {
        bars = await fetchJson('/api/market-data/' + encodeURIComponent(symbol) + '/daily' + query);
      }
      renderDailyPriceChart(chart, bars);
    } catch (err) {
      chart.innerHTML = '<span class="error">Daily bars unavailable: ' + escapeHtml(err.message) + '</span>';
    }
  }

  function renderDailyPriceChart(chart, rows) {
    const bars = (rows || []).map(normalizeMarketBar)
      .filter((bar) => bar && bar.date && Number.isFinite(bar.close) && bar.close > 0)
      .sort((a, b) => String(a.date).localeCompare(String(b.date)))
      .slice(-90);
    if (bars.length === 0) {
      chart.textContent = 'No daily bars available.';
      return;
    }
    const closes = bars.map((bar) => bar.close);
    const min = Math.min(...closes);
    const max = Math.max(...closes);
    const span = max - min || 1;
    const width = 640;
    const height = 180;
    const points = bars.map((bar, index) => {
      const x = bars.length === 1 ? width : (index / (bars.length - 1)) * width;
      const y = height - ((bar.close - min) / span) * (height - 18) - 9;
      return x.toFixed(1) + ',' + y.toFixed(1);
    }).join(' ');
    const first = bars[0];
    const last = bars[bars.length - 1];
    const change = first.close === 0 ? 0 : (last.close - first.close) / first.close;
    const stroke = change >= 0 ? 'var(--pass)' : 'var(--reject)';
    chart.innerHTML = `
      <div class="chart-meta">
        <span>${escapeHtml(first.date)} to ${escapeHtml(last.date)}</span>
        <span><strong>${fmtMoney(last.close)}</strong> ${fmtPct(change)}</span>
        <span>Vol <strong>${fmtCompact(last.volume)}</strong></span>
      </div>
      <svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" role="img" aria-label="Daily close price chart">
        <polyline fill="none" stroke="${stroke}" stroke-width="3" vector-effect="non-scaling-stroke" points="${points}"></polyline>
        <line x1="0" y1="${height - 1}" x2="${width}" y2="${height - 1}" stroke="var(--border)" vector-effect="non-scaling-stroke"></line>
      </svg>
    `;
  }

  async function saveNote(event, candidateId) {
    event.preventDefault();
    const form = event.target;
    const data = Object.fromEntries(new FormData(form));
    $('#note-status').textContent = 'Saving...';
    try {
      await sendJson('/api/candidates/' + encodeURIComponent(candidateId) + '/notes', 'POST', {
        note: data.note,
        author: data.author
      });
      $('#note-status').textContent = 'Saved';
      await reloadSelectedCandidate();
    } catch (err) {
      $('#note-status').textContent = 'Failed: ' + err.message;
    }
  }

  async function saveOverride(event, verdict) {
    event.preventDefault();
    if (!verdict || !verdict.verdictId) return;
    const form = event.target;
    const data = Object.fromEntries(new FormData(form));
    $('#override-status').textContent = 'Saving...';
    try {
      await sendJson('/api/verdicts/' + encodeURIComponent(verdict.verdictId) + '/override', 'POST', {
        verdict: data.verdict,
        reason: data.reason,
        author: data.author
      });
      $('#override-status').textContent = 'Saved';
      await reloadSelectedCandidate();
      await loadCandidates();
    } catch (err) {
      $('#override-status').textContent = 'Failed: ' + err.message;
    }
  }

  async function reloadSelectedCandidate() {
    if (!selectedCandidateId) return;
    renderCandidateDetail(await fetchJson('/api/candidates/id/' + encodeURIComponent(selectedCandidateId)));
    if (selectedCandidateCard) {
      selectedCandidateCard.classList.add('selected');
    }
  }

  function detailField(label, value) {
    return '<div class="detail-field"><span>' + escapeHtml(label) + '</span><strong>'
      + escapeHtml(value || '-') + '</strong></div>';
  }

  function detailTable(title, headers, rows) {
    const body = rows && rows.length
      ? rows.map((row) => '<tr>' + row.map((cell) => '<td>' + escapeHtml(cell) + '</td>').join('') + '</tr>').join('')
      : '<tr><td colspan="' + headers.length + '" style="color:var(--fg-muted);">No data.</td></tr>';
    return `
      <div class="table-wrap">
        <h3>${escapeHtml(title)}</h3>
        <table>
          <thead><tr>${headers.map((header) => '<th>' + escapeHtml(header) + '</th>').join('')}</tr></thead>
          <tbody>${body}</tbody>
        </table>
      </div>
    `;
  }

  async function loadOpportunityQuality() {
    const lookbackHours = Math.max(1, parseInt(lookbackEl.value || '24', 10));
    const lookbackDays = Math.max(1, Math.ceil(lookbackHours / 24));
    try {
      const quality = await fetchJson('/api/stats/opportunity-quality?lookbackDays=' + lookbackDays + '&topSymbols=10');
      const bands = quality.convictionBands || {};
      $('#quality-bands').innerHTML = [
        '<span class="chip pass">High ' + fmtInt(bands.high) + '</span>',
        '<span class="chip watch">Medium ' + fmtInt(bands.medium) + '</span>',
        '<span class="chip reject">Low ' + fmtInt(bands.low) + '</span>'
      ].join('');

      const verdictMix = quality.verdictMix || {};
      const mixEntries = Object.entries(verdictMix);
      $('#quality-verdict-mix').innerHTML = mixEntries.length
        ? mixEntries.map(([name, count]) => '<span class="chip">' + escapeHtml(name) + ' ' + fmtInt(count) + '</span>').join('')
        : '<span class="chip">No data</span>';

      const symbolRows = quality.topSymbolsByAverageScore || [];
      $('#quality-symbols').innerHTML = symbolRows.length
        ? symbolRows.map((row) => `
            <tr>
              <td>${escapeHtml(row.symbol)}</td>
              <td>${fmtScore(row.avgScore)}</td>
              <td>${fmtInt(row.sampleSize)}</td>
            </tr>
          `).join('')
        : '<tr><td colspan="3" style="color:var(--fg-muted);">No symbol score data.</td></tr>';
    } catch (err) {
      $('#quality-bands').innerHTML = '<span class="chip error">Failed to load</span>';
      $('#quality-verdict-mix').innerHTML = '';
      $('#quality-symbols').innerHTML = '<tr><td colspan="3" class="error">Unavailable</td></tr>';
    }
  }

  async function loadPerformance() {
    try {
      const [top, worst] = await Promise.all([
        fetchJson('/api/calibration/outcomes/top?limit=10'),
        fetchJson('/api/calibration/outcomes/worst?limit=10')
      ]);
      renderOutcomeRows('#top-outcomes', top);
      renderOutcomeRows('#worst-outcomes', worst);
    } catch (err) {
      $('#top-outcomes').innerHTML = '<tr><td colspan="4" class="error">Unavailable</td></tr>';
      $('#worst-outcomes').innerHTML = '<tr><td colspan="4" class="error">Unavailable</td></tr>';
    }
  }

  function renderOutcomeRows(selector, rows) {
    const body = $(selector);
    body.innerHTML = rows && rows.length
      ? rows.map((row) => `
          <tr>
            <td>${escapeHtml(row.symbol)}</td>
            <td>${fmtPct(row.realizedReturn)}</td>
            <td>${fmtInt(row.daysHeld)}</td>
            <td>${row.thesisWorked ? 'Yes' : 'No'}</td>
          </tr>
        `).join('')
      : '<tr><td colspan="4" style="color:var(--fg-muted);">No outcomes yet.</td></tr>';
  }

  async function loadOutcomesJournal(reset) {
    if (reset) {
      outcomesOffset = 0;
      outcomesHasMore = true;
      $('#outcomes-journal').innerHTML = '';
    }
    if (!outcomesHasMore) return;

    $('#outcomes-status').textContent = 'Loading...';
    try {
      const rows = await fetchJson('/api/calibration/outcomes/page?offset=' + outcomesOffset + '&limit=' + outcomesLimit);
      const html = rows.map((row) => `
        <tr>
          <td>${escapeHtml(fmtDate(row.observedAt))}</td>
          <td>${escapeHtml(row.symbol)}</td>
          <td>${fmtPct(row.realizedReturn)}</td>
          <td>${fmtPct(row.maxDrawdown)}</td>
          <td>${fmtInt(row.daysHeld)}</td>
          <td>${row.thesisWorked ? 'Yes' : 'No'}</td>
        </tr>
      `).join('');

      if (reset && rows.length === 0) {
        $('#outcomes-journal').innerHTML = '<tr><td colspan="6" style="color:var(--fg-muted);">No outcomes recorded.</td></tr>';
      } else {
        $('#outcomes-journal').insertAdjacentHTML('beforeend', html);
      }

      outcomesOffset += rows.length;
      outcomesHasMore = rows.length === outcomesLimit;
      $('#outcomes-status').textContent = outcomesHasMore ? fmtInt(outcomesOffset) + ' loaded' : 'End of journal';
      outcomesLoadMoreBtn.disabled = !outcomesHasMore;
    } catch (err) {
      $('#outcomes-status').textContent = 'Failed to load journal';
      if (reset) {
        $('#outcomes-journal').innerHTML = '<tr><td colspan="6" class="error">Unavailable</td></tr>';
      }
    }
  }

  async function downloadOutcomesCsv(event) {
    event.preventDefault();
    try {
      const res = await fetch('/api/calibration/outcomes/export?limit=500', { headers: authHeaders() });
      if (!res.ok) throw new Error(res.status + ' ' + res.statusText);
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'calibration-outcomes.csv';
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      $('#outcomes-status').textContent = 'CSV export failed: ' + err.message;
    }
  }

  function refreshAll() {
    loadValidationProfiles();
    loadBacktestRuns();
    loadPipelineStatus();
    loadStats();
    loadCalibrationReport();
    loadCandidates();
    loadOpportunityQuality();
    loadPerformance();
    loadTradeHistory();
    loadOutcomesJournal(true);
    $('#outcomes-export').href = '/api/calibration/outcomes/export?limit=500';
    connectCandidateStream();
  }

  $('#exit-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target;
    const data = Object.fromEntries(new FormData(form));
    const body = {
      equilibriumPhase: data.equilibriumPhase,
      propagationPhase: data.propagationPhase,
      currentAsymmetry: parseFloat(data.currentAsymmetry),
      entryAsymmetry: parseFloat(data.entryAsymmetry),
      rangePosition: parseFloat(data.rangePosition),
      nearRecentHigh: data.nearRecentHigh === 'true'
    };
    const out = $('#exit-output');
    out.className = 'exit-output';
    out.innerHTML = 'Evaluating...';
    try {
      const res = await fetch('/api/exit-signal', {
        method: 'POST',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify(body)
      });
      if (!res.ok) throw new Error(res.status + ' ' + res.statusText);
      const signal = await res.json();
      out.className = 'exit-output ' + signal.recommendation;
      out.innerHTML = `
        <strong>${escapeHtml(signal.recommendation)}</strong>
        <span style="color:var(--fg-muted)"> compression score ${fmtScore(signal.compressionScore)}</span>
        <ul>${(signal.notes || []).map((note) => '<li>' + escapeHtml(note) + '</li>').join('')}</ul>
      `;
    } catch (err) {
      out.className = 'exit-output';
      out.innerHTML = '<span class="error">Failed: ' + escapeHtml(err.message) + '</span>';
    }
  });

  fillBacktestSample();
  fillTradeHistorySample();
  fillMarketStateSample();
  refreshAll();