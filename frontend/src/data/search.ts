const characterFoldingMap = new Map([
  [0xF2, 'o'], // ò
  [0xF3, 'o'], // ó
  [0xF4, 'o'], // ô
  [0xF5, 'o'], // õ
  [0xF6, 'o'], // ö
  [0xF8, 'o'], // ø
  [0xE0, 'a'], // à
  [0xE1, 'a'], // á
  [0xE2, 'a'], // â
  [0xE3, 'a'], // ã
  [0xE4, 'a'], // ä
  [0xE5, 'a'], // å
  [0xF9, 'u'], // ù
  [0xFA, 'u'], // ú
  [0xFB, 'u'], // û
  [0xFC, 'u'], // ü
]);

export const tokenizeString = (str: string): string => {
  return str
    .toLowerCase()
    .replace(/./g, char => characterFoldingMap.get(char.charCodeAt(0)) ?? char)
    .replace(/[^a-z0-9]/g, '');
};
