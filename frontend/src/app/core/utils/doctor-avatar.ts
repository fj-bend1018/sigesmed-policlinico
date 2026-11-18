const MALE_AVATARS = [
  '/assets/rediseno/doc-male-1.jpg',
  '/assets/rediseno/doc-male-2.jpg',
  '/assets/rediseno/doc-male-3.jpg'
];

const FEMALE_AVATARS = [
  '/assets/rediseno/doc-female-1.jpg',
  '/assets/rediseno/doc-female-2.jpg',
  '/assets/rediseno/doc-female-3.jpg'
];

const FEMALE_NAME_HINTS = [
  'carmen', 'lucia', 'patricia', 'maria', 'sofia', 'valeria', 'andrea', 'elena',
  'gabriela', 'claudia', 'daniela', 'monica', 'camila', 'natalia', 'ximena',
  'paola', 'veronica', 'teresa', 'rosa', 'isabel', 'ana', 'laura', 'carla',
  'diana', 'julia', 'silvia', 'gloria', 'marta', 'beatriz', 'fernanda',
  'alejandra', 'milagros', 'lucero', 'rocio', 'cristina', 'mariana', 'karina',
  'lorena', 'adriana', 'vanessa', 'jimena', 'fabiola', 'rocio'
];

export function isFemaleDoctorName(name?: string | null): boolean {
  if (!name) return false;
  const normalized = name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
  if (/^dra\.?\b/.test(normalized)) return true;
  if (/^dr\.?\b/.test(normalized)) return false;
  const tokens = normalized.replace(/^(lic|ps|mg)\.?\s+/i, '').split(/\s+/);
  return tokens.some(token => FEMALE_NAME_HINTS.includes(token));
}

export function doctorAvatarUrl(
  name?: string | null,
  index = 0,
  doctorsList?: ReadonlyArray<{ nombre?: string | null }>
): string {
  const female = isFemaleDoctorName(name);
  const pool = female ? FEMALE_AVATARS : MALE_AVATARS;
  if (doctorsList && index >= 0 && index < doctorsList.length) {
    let sameGenderBefore = 0;
    for (let i = 0; i < index; i++) {
      if (isFemaleDoctorName(doctorsList[i]?.nombre) === female) {
        sameGenderBefore++;
      }
    }
    return pool[sameGenderBefore % pool.length];
  }
  if (name) {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = (hash * 31 + name.charCodeAt(i)) >>> 0;
    }
    return pool[(hash + index) % pool.length];
  }
  return pool[Math.abs(index) % pool.length];
}
