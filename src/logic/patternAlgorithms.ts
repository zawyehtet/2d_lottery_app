import { BetItem, MAX_BET_AMOUNT } from '../types';

export function toAsciiDigits(input: string): string {
  const myanmarDigits = '၀၁၂၃၄၅၆၇၈၉';
  const asciiDigits = '0123456789';
  let out = '';
  for (let i = 0; i < input.length; i++) {
    const char = input[i];
    const idx = myanmarDigits.indexOf(char);
    out += idx !== -1 ? asciiDigits[idx] : char;
  }
  return out;
}

export function validateBetAmount(rawInput: string | number | null | undefined): { isValid: boolean; error?: string } {
  if (rawInput === null || rawInput === undefined) return { isValid: false, error: 'Empty' };
  const trimmed = toAsciiDigits(String(rawInput)).trim();
  if (trimmed === '') return { isValid: false, error: 'Empty' };

  if (trimmed.includes('/')) {
    const parts = trimmed.split('/');
    if (parts.length !== 2) return { isValid: false, error: 'NonNumeric' };
    const p1 = parts[0].trim();
    const p2 = parts[1].trim();
    if (!p1 || !p2) return { isValid: false, error: 'Empty' };
    const v1 = validateBetAmount(p1);
    if (!v1.isValid) return v1;
    const v2 = validateBetAmount(p2);
    if (!v2.isValid) return v2;
    return { isValid: true };
  }

  const isNegative = trimmed.startsWith('-');
  const digitsPart = isNegative ? trimmed.slice(1) : trimmed;
  if (!/^\d+$/.test(digitsPart)) {
    return { isValid: false, error: 'NonNumeric' };
  }
  if (isNegative) {
    return { isValid: false, error: 'ZeroOrNegative' };
  }
  const parsed = parseInt(trimmed, 10);
  if (isNaN(parsed) || parsed <= 0) {
    return { isValid: false, error: 'ZeroOrNegative' };
  }
  if (parsed > MAX_BET_AMOUNT) {
    return { isValid: false, error: 'TooLarge' };
  }
  return { isValid: true };
}

export const POWER_NUMBERS = [
  '05', '50', '16', '61', '27', '72', '38', '83', '49', '94',
];

export const NAKHAT_NUMBERS = [
  '07', '70', '18', '81', '24', '42', '35', '53', '69', '96',
];

export const BROTHER_NUMBERS = [
  '01', '10', '12', '21', '23', '32', '34', '43', '45', '54',
  '56', '65', '67', '76', '78', '87', '89', '98', '90', '09',
];

export const DOUBLES = [
  '00', '11', '22', '33', '44', '55', '66', '77', '88', '99',
];

export function reverse2D(n: string): string {
  return n.length === 2 ? `${n[1]}${n[0]}` : n;
}

export function getEvenEven(): string[] {
  const evens = ['0', '2', '4', '6', '8'];
  const res: string[] = [];
  for (const d1 of evens) {
    for (const d2 of evens) {
      res.push(`${d1}${d2}`);
    }
  }
  return res;
}

export function getOddOdd(): string[] {
  const odds = ['1', '3', '5', '7', '9'];
  const res: string[] = [];
  for (const d1 of odds) {
    for (const d2 of odds) {
      res.push(`${d1}${d2}`);
    }
  }
  return res;
}

export function getEvenOdd(): string[] {
  const evens = ['0', '2', '4', '6', '8'];
  const odds = ['1', '3', '5', '7', '9'];
  const res: string[] = [];
  for (const d1 of evens) {
    for (const d2 of odds) {
      res.push(`${d1}${d2}`);
    }
  }
  return res;
}

export function getOddEven(): string[] {
  const odds = ['1', '3', '5', '7', '9'];
  const evens = ['0', '2', '4', '6', '8'];
  const res: string[] = [];
  for (const d1 of odds) {
    for (const d2 of evens) {
      res.push(`${d1}${d2}`);
    }
  }
  return res;
}

export function getHead(d: string): string[] {
  const res: string[] = [];
  const char = d[0] || '0';
  for (let i = 0; i <= 9; i++) {
    res.push(`${char}${i}`);
  }
  return res;
}

export function getTail(d: string): string[] {
  const res: string[] = [];
  const char = d[0] || '0';
  for (let i = 0; i <= 9; i++) {
    res.push(`${i}${char}`);
  }
  return res;
}

export function getIncludes(d: string): string[] {
  const char = d[0] || '0';
  const set = new Set<string>();
  for (let i = 0; i <= 9; i++) {
    set.add(`${char}${i}`);
    set.add(`${i}${char}`);
  }
  return Array.from(set);
}

export function getBrake(brake: number): string[] {
  const target = ((brake % 10) + 10) % 10;
  const res: string[] = [];
  for (let i = 0; i <= 9; i++) {
    for (let j = 0; j <= 9; j++) {
      if ((i + j) % 10 === target) {
        res.push(`${i}${j}`);
      }
    }
  }
  return res;
}

export function getKway(digitsStr: string, withDoubles: boolean): string[] {
  const distinct = Array.from(new Set(digitsStr.split('').filter((c) => /\d/.test(c))));
  if (distinct.length < 2) return [];
  const res: string[] = [];
  for (let i = 0; i < distinct.length; i++) {
    for (let j = 0; j < distinct.length; j++) {
      if (i !== j || withDoubles) {
        res.push(`${distinct[i]}${distinct[j]}`);
      }
    }
  }
  return res;
}

export function generateNumbersForPattern(
  pattern: string,
  inputNumber: string,
  withReverse: boolean,
  withDoublesForKway = false
): string[] {
  let baseList: string[] = [];
  switch (pattern) {
    case 'ဒဲ့':
      baseList = inputNumber.length === 2 ? [inputNumber] : [];
      break;
    case 'ပါဝါ':
      baseList = [...POWER_NUMBERS];
      break;
    case 'နက္ခတ်':
      baseList = [...NAKHAT_NUMBERS];
      break;
    case 'ညီအစ်ကို':
      baseList = [...BROTHER_NUMBERS];
      break;
    case 'အပူး':
      baseList = [...DOUBLES];
      break;
    case 'စုံစုံ':
      baseList = getEvenEven();
      break;
    case 'မမ':
      baseList = getOddOdd();
      break;
    case 'စုံမ':
      baseList = getEvenOdd();
      break;
    case 'မစုံ':
      baseList = getOddEven();
      break;
    case 'ထိပ်':
      baseList = getHead(inputNumber[0] || '0');
      break;
    case 'နောက်':
      baseList = getTail(inputNumber[0] || '0');
      break;
    case 'အပါ':
      baseList = getIncludes(inputNumber[0] || '0');
      break;
    case 'ဘရိတ်': {
      const b = parseInt(inputNumber[0] || '0', 10);
      baseList = getBrake(isNaN(b) ? 0 : b);
      break;
    }
    case 'ခွေ':
      baseList = getKway(inputNumber, withDoublesForKway);
      break;
    default:
      baseList = inputNumber.length === 2 ? [inputNumber] : [];
  }

  if (!withReverse) {
    return baseList;
  }

  const set = new Set<string>();
  for (const n of baseList) {
    set.add(n);
    set.add(reverse2D(n));
  }
  return Array.from(set);
}

export function parseBetInput(input: string): Array<{ n: string; a: number }> {
  const trimmed = toAsciiDigits(input).trim().replace(/\s*([=/])\s*/g, '$1');
  if (!trimmed) return [];

  // Case 2 & 3: Split amount with slash ^(\d{2})=(\d+)/(\d+)$
  const splitMatch = trimmed.match(/^(\d{2})=(\d+)\/(\d+)$/);
  if (splitMatch) {
    const baseNumber = splitMatch[1];
    const directAmount = parseInt(splitMatch[2], 10) || 0;
    const reverseAmount = parseInt(splitMatch[3], 10) || 0;
    const reverseNumber = reverse2D(baseNumber);

    const entries: Array<{ n: string; a: number }> = [];
    if (baseNumber === reverseNumber) {
      entries.push({ n: baseNumber, a: directAmount + reverseAmount });
    } else {
      entries.push({ n: baseNumber, a: directAmount });
      if (reverseAmount > 0) {
        entries.push({ n: reverseNumber, a: reverseAmount });
      }
    }
    return entries;
  }

  // Case 1: Standard single amount ^(\d{2})=(\d+)$
  const singleMatch = trimmed.match(/^(\d{2})=(\d+)$/);
  if (singleMatch) {
    const baseNumber = singleMatch[1];
    const amount = parseInt(singleMatch[2], 10) || 0;
    return [{ n: baseNumber, a: amount }];
  }

  return [];
}

function stripCommentAndGuideLines(text: string): string {
  return text
    .split('\n')
    .map((line) => line.replace(/\([^)]*\)/g, '').trim())
    .filter((line) => {
      if (!line) return false;
      if (line.startsWith('(') || line.endsWith(')')) return false;
      if (/^[=\-\s]+$/.test(line)) return false;
      return true;
    })
    .join('\n');
}

export interface ParseResult {
  items: Array<{ n: string; a: number }>;
  skippedCount: number;
  blockedSkipped: string[];
  invalidTokens: string[];
}

export function parseQuickEntryMode1(
  text: string,
  blocked: Set<string>,
  maxBet: number = MAX_BET_AMOUNT
): ParseResult {
  const items: Array<{ n: string; a: number }> = [];
  let skipped = 0;
  const blockedList: string[] = [];
  const invalidTokens: string[] = [];

  const cleanedText = stripCommentAndGuideLines(toAsciiDigits(text))
    .replace(/ဒဲ့ကျန်/g, '/')
    .replace(/ဒဲ့/g, '=')
    .replace(/ကျန်/g, '/')
    .replace(/\/[rR]/g, '/');

  if (!cleanedText.trim()) {
    return { items, skippedCount: skipped, blockedSkipped: blockedList, invalidTokens };
  }

  const normalizedLines = cleanedText.split('\n').map((line) => {
    let l = line.trim();
    l = l.replace(/([=])(\d+)[rR](\d+)/g, '$1$2/$3');
    l = l.replace(/((?:\b\d{2}[.,\s]+)*\b\d{2})[-]+(?=\d)/g, '$1=');
    l = l.replace(/((?:\b\d{2}[.,\s]+)*\b\d{2})[.](?=\d+\/\d+)/g, '$1=');
    l = l.replace(/((?:\b\d{2}[.,\s]+)*\b\d{2})\s+(?=\d+\/\d+)/g, '$1=');
    l = l.replace(/(\b\d{2})[\s,]+(?=\d{2}(?:[.\s,]+\d{2})*\s*[=rR])/g, '$1.');
    l = l.replace(/\s*([=rR/])\s*/g, '$1');
    l = l.replace(/([0-9.,\s]+)[rR]=\s*([0-9]+)/g, '$1R$2');
    l = l.replace(/([0-9.,\s]+)=\s*[rR]([0-9]+)/g, '$1R$2');
    l = l.replace(/([0-9.,\s]+)=\s*([0-9]+)[rR]/g, '$1R$2');
    return l;
  });

  const tokens = normalizedLines
    .flatMap((l) => l.split(/[\s;|]+|,(?=\s|$|[0-9]{1,2}[.=rR]|[0-9][abtwABTW]|[pnsxPNSX][0-9])/))
    .map((t) => t.trim())
    .filter(Boolean);

  for (const token of tokens) {
    try {
      // Pattern 3: Equal and slash (e.g., 01.78=500/100 or 56=2000/1000)
      if (token.includes('=') && token.includes('/')) {
        const eqParts = token.split('=');
        if (eqParts.length === 2) {
          const numPart = eqParts[0];
          const amtParts = eqParts[1].split('/');
          const directRaw = amtParts[0];
          const reverseRaw = amtParts[1];
          const directVal = validateBetAmount(directRaw);
          const amtDirect = parseInt(directRaw?.trim() || '0', 10) || 0;
          const amtReverse = parseInt(reverseRaw?.trim() || '0', 10);
          const reverseValid = amtReverse === 0 || (validateBetAmount(reverseRaw).isValid && amtReverse <= maxBet);
          const rawNumbers = numPart.split(/[.,\s]+/).filter(Boolean);
          const validNumbers: string[] = [];
          for (const raw of rawNumbers) {
            if (raw.length === 2 && /^\d+$/.test(raw)) {
              validNumbers.push(raw);
            } else {
              invalidTokens.push(raw);
            }
          }

          if (validNumbers.length === 0 || !directVal.isValid || amtDirect > maxBet || !reverseValid) {
            skipped++;
          } else {
            for (const n of validNumbers) {
              if (blocked.has(n)) {
                blockedList.push(n);
              } else {
                items.push({ n, a: amtDirect });
              }
              const rev = reverse2D(n);
              if (rev === n) {
                if (validNumbers.length === 1 && amtReverse > 0) {
                  if (blocked.has(n)) {
                    blockedList.push(n);
                  } else {
                    items.push({ n, a: amtReverse });
                  }
                }
              } else if (amtReverse > 0) {
                if (blocked.has(rev)) {
                  blockedList.push(rev);
                } else {
                  items.push({ n: rev, a: amtReverse });
                }
              }
            }
          }
          continue;
        }
      }

      // Pattern 2: R notation (e.g. 01.78R500 or 33R3000)
      if (/r/i.test(token)) {
        const rIndex = token.search(/r/i);
        const numPart = token.slice(0, rIndex);
        const amtPart = token.slice(rIndex + 1);
        const amtVal = validateBetAmount(amtPart);
        const amount = parseInt(amtPart.trim(), 10) || 0;
        const rawNumbers = numPart.split(/[.,\s]+/).filter(Boolean);
        const validNumbers: string[] = [];
        for (const raw of rawNumbers) {
          if (raw.length === 2 && /^\d+$/.test(raw)) {
            validNumbers.push(raw);
          } else {
            invalidTokens.push(raw);
          }
        }
        if (validNumbers.length === 0 || !amtVal.isValid || amount > maxBet) {
          skipped++;
        } else {
          for (const n of validNumbers) {
            const rev = reverse2D(n);
            const listToBet = n === rev ? [n, n] : [n, rev];
            for (const num of listToBet) {
              if (blocked.has(num)) {
                blockedList.push(num);
              } else {
                items.push({ n: num, a: amount });
              }
            }
          }
        }
        continue;
      }

      // Pattern 1: Same amount with '=' (e.g. 01.78=500)
      if (token.includes('=')) {
        const parts = token.split('=');
        if (parts.length === 2) {
          const numPart = parts[0];
          const amtVal = validateBetAmount(parts[1]);
          const amount = parseInt(parts[1].trim(), 10) || 0;
          const rawNumbers = numPart.split(/[.,\s]+/).filter(Boolean);
          const validNumbers: string[] = [];
          for (const raw of rawNumbers) {
            if (raw.length === 2 && /^\d+$/.test(raw)) {
              validNumbers.push(raw);
            } else {
              invalidTokens.push(raw);
            }
          }
          if (validNumbers.length === 0 || !amtVal.isValid || amount > maxBet) {
            skipped++;
          } else {
            for (const n of validNumbers) {
              if (blocked.has(n)) {
                blockedList.push(n);
              } else {
                items.push({ n, a: amount });
              }
            }
          }
          continue;
        }
      }

      invalidTokens.push(token);
      skipped++;
    } catch {
      invalidTokens.push(token);
      skipped++;
    }
  }

  return { items, skippedCount: skipped, blockedSkipped: blockedList, invalidTokens };
}

export function parseQuickEntryMode2(
  text: string,
  blocked: Set<string>,
  maxBet: number = MAX_BET_AMOUNT
): ParseResult {
  const items: Array<{ n: string; a: number }> = [];
  let skipped = 0;
  const blockedList: string[] = [];
  const invalidTokens: string[] = [];

  const cleanedText = stripCommentAndGuideLines(toAsciiDigits(text))
    .replace(/ဒဲ့ကျန်/g, '/')
    .replace(/ဒဲ့/g, '=')
    .replace(/ကျန်/g, '/')
    .replace(/\/[rR]/g, '/');

  if (!cleanedText.trim()) {
    return { items, skippedCount: skipped, blockedSkipped: blockedList, invalidTokens };
  }

  const normalizedLines = cleanedText.split('\n').map((line) => {
    let l = line.trim();
    l = l.replace(/([=])(\d+)[rR](\d+)/g, '$1$2/$3');
    l = l.replace(/((?:\b\d{2}[.,\s]+)*\b\d{2})[-]+(?=\d)/g, '$1=');
    l = l.replace(/((?:\b\d{2}[.,\s]+)*\b\d{2})[.](?=\d+\/\d+)/g, '$1=');
    l = l.replace(/((?:\b\d{2}[.,\s]+)*\b\d{2})\s+(?=\d+\/\d+)/g, '$1=');
    l = l.replace(/(\b\d{2})[\s,]+(?=\d{2}(?:[.\s,]+\d{2})*\s*[=rR])/g, '$1.');
    l = l.replace(/\s*([=rR/])\s*/g, '$1');
    l = l.replace(/([0-9.,\s]+)[rR]=\s*([0-9]+)/g, '$1R$2');
    l = l.replace(/([0-9.,\s]+)=\s*[rR]([0-9]+)/g, '$1R$2');
    l = l.replace(/([0-9.,\s]+)=\s*([0-9]+)[rR]/g, '$1R$2');
    return l;
  });

  const tokens = normalizedLines
    .flatMap((l) => l.split(/[\s;|]+|,(?=\s|$|[0-9]{1,2}[.=rR]|[0-9][abtwABTW]|[pnsxPNSX][0-9])/))
    .map((t) => t.trim())
    .filter(Boolean);

  for (const rawToken of tokens) {
    if (rawToken.startsWith('(') || rawToken.endsWith(')')) continue;
    const token = rawToken.toLowerCase().trim();

    try {
      // Standard formats in Fast Bet 2
      if (token.includes('=') || (token.includes('r') && /^\d/.test(token))) {
        const res = parseQuickEntryMode1(token, blocked, maxBet);
        items.push(...res.items);
        skipped += res.skippedCount;
        blockedList.push(...res.blockedSkipped);
        invalidTokens.push(...res.invalidTokens);
        continue;
      }

      // Digit + letter + amount: a (အပါ), b (ဘရိတ်), t (ထိပ်), w (နောက်)
      const matchDigitLetterAmt = token.match(/^(\d)([abtw])(\d+)$/);
      if (matchDigitLetterAmt) {
        const digitChar = matchDigitLetterAmt[1];
        const letter = matchDigitLetterAmt[2];
        const rawAmt = matchDigitLetterAmt[3];
        const amtVal = validateBetAmount(rawAmt);
        const amount = parseInt(rawAmt, 10) || 0;

        if (!amtVal.isValid || amount > maxBet) {
          skipped++;
          invalidTokens.push(rawToken);
          continue;
        }

        let nums: string[] = [];
        if (letter === 'a') nums = getIncludes(digitChar);
        else if (letter === 'b') nums = getBrake(parseInt(digitChar, 10));
        else if (letter === 't') nums = getHead(digitChar);
        else if (letter === 'w') nums = getTail(digitChar);

        for (const n of nums) {
          if (blocked.has(n)) {
            blockedList.push(n);
          } else {
            items.push({ n, a: amount });
          }
        }
        continue;
      }

      // Digits (2..9) + k (ခွေ) + amount: e.g. 123k500
      const matchKwayAmt = token.match(/^(\d{2,9})k(\d+)$/);
      if (matchKwayAmt) {
        const digitsStr = matchKwayAmt[1];
        const rawAmt = matchKwayAmt[2];
        const amtVal = validateBetAmount(rawAmt);
        const amount = parseInt(rawAmt, 10) || 0;
        const nums = getKway(digitsStr, false);

        if (!amtVal.isValid || amount > maxBet || nums.length === 0) {
          skipped++;
          invalidTokens.push(rawToken);
          continue;
        }

        for (const n of nums) {
          if (blocked.has(n)) {
            blockedList.push(n);
          } else {
            items.push({ n, a: amount });
          }
        }
        continue;
      }

      // Letter + amount: p/n/s/x
      const matchLetterAmt = token.match(/^(?:\d)?([pnsx])(\d+)$/);
      if (matchLetterAmt) {
        const letter = matchLetterAmt[1];
        const rawAmt = matchLetterAmt[2];
        const amtVal = validateBetAmount(rawAmt);
        const amount = parseInt(rawAmt, 10) || 0;

        if (!amtVal.isValid || amount > maxBet) {
          skipped++;
          invalidTokens.push(rawToken);
          continue;
        }

        let nums: string[] = [];
        if (letter === 'p') nums = [...POWER_NUMBERS];
        else if (letter === 'n') nums = [...NAKHAT_NUMBERS];
        else if (letter === 's') nums = [...DOUBLES];
        else if (letter === 'x') nums = [...BROTHER_NUMBERS];

        for (const n of nums) {
          if (blocked.has(n)) {
            blockedList.push(n);
          } else {
            items.push({ n, a: amount });
          }
        }
        continue;
      }

      invalidTokens.push(rawToken);
      skipped++;
    } catch {
      invalidTokens.push(rawToken);
      skipped++;
    }
  }

  return { items, skippedCount: skipped, blockedSkipped: blockedList, invalidTokens };
}

export function generateSorted2DLedger(currentSalesMap: Record<string, number>): Array<{ n: string; a: number }> {
  const allNumbers: Array<{ n: string; a: number }> = [];
  for (let i = 0; i <= 99; i++) {
    const numStr = i.toString().padStart(2, '0');
    allNumbers.push({
      n: numStr,
      a: currentSalesMap[numStr] || 0,
    });
  }

  const activeBets = allNumbers.filter((x) => x.a > 0);
  const zeroBets = allNumbers.filter((x) => x.a === 0);

  activeBets.sort((x, y) => {
    if (y.a !== x.a) return y.a - x.a; // Descending by bet amount
    return parseInt(x.n, 10) - parseInt(y.n, 10); // Ascending by number
  });

  zeroBets.sort((x, y) => parseInt(x.n, 10) - parseInt(y.n, 10));

  return [...activeBets, ...zeroBets];
}
