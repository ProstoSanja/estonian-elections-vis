function processName(name) {
  return foldMaintaining(name.trim().toLowerCase()).replace("-", " ").split(" ")
}

export {processName};

function foldMaintaining(str = '') {
  return _fold(str, (char) => char);
}

function _fold(str) {
  if (str === null)
    return '';

  if (typeof str === 'number')
    return '' + str;

  if (typeof str !== 'string')
    throw new Error('Invalid input data type');

  return str.split('').map(character => {
    if (character.charCodeAt(0) < 128) {
      return character;
    } else {
      const replacement = mappings.get(character.charCodeAt(0));
      return (replacement === undefined) ? character : replacement;
    }
  }).join('');
}

const mappings = new Map([
  [0xF2, 'o'],
  [0xF3, 'o'],
  [0xF4, 'o'],
  [0xF5, 'o'],
  [0xF6, 'o'],
  [0xF8, 'o'],
  [0xE0, 'a'],
  [0xE1, 'a'],
  [0xE2, 'a'],
  [0xE3, 'a'],
  [0xE4, 'a'],
  [0xE5, 'a'],
  [0xF9, 'u'],
  [0xFA, 'u'],
  [0xFB, 'u'],
  [0xFC, 'u'],
]);