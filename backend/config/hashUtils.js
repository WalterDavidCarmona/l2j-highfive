/**
 * Utilidades de hash compatibles con L2JMobius
 * El servidor usa SHA1 por defecto para passwords de cuentas
 */
const crypto = require('crypto');
const bcrypt = require('bcryptjs');

const HASH_TYPE = process.env.L2_PASS_HASH || 'sha1';

/**
 * Genera hash compatible con L2JMobius
 */
function hashPassword(plainPassword) {
  switch (HASH_TYPE) {
    case 'sha1':
      return crypto.createHash('sha1').update(plainPassword).digest('base64');
    case 'md5':
      return crypto.createHash('md5').update(plainPassword).digest('base64');
    case 'bcrypt':
      return bcrypt.hashSync(plainPassword, 10);
    default:
      return crypto.createHash('sha1').update(plainPassword).digest('hex');
  }
}

/**
 * Verifica password contra el hash almacenado
 */
function verifyPassword(plainPassword, storedHash) {
  switch (HASH_TYPE) {
    case 'sha1': {
      const hash = crypto.createHash('sha1').update(plainPassword).digest('base64');
      return hash === storedHash;
    }
    case 'md5': {
      const hash = crypto.createHash('md5').update(plainPassword).digest('base64');
      return hash === storedHash;
    }
    case 'bcrypt':
      return bcrypt.compareSync(plainPassword, storedHash);
    default: {
      const hash = crypto.createHash('sha1').update(plainPassword).digest('base64');
      return hash === storedHash;
    }
  }
}

module.exports = { hashPassword, verifyPassword };
