const mysql = require('mysql2/promise');
require('dotenv').config();

const pool = mysql.createPool({
  host:     process.env.DB_HOST     || 'localhost',
  port:     parseInt(process.env.DB_PORT) || 3306,
  user:     process.env.DB_USER     || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME     || 'l2jmobiush5',
  waitForConnections: true,
  connectionLimit: 20,
  queueLimit: 0,
  charset: 'utf8mb4',
  timezone: 'Z'
});

// Test de conexión al iniciar
pool.getConnection()
  .then(conn => {
    console.log('✅ Conectado a MySQL:', process.env.DB_NAME || 'l2jmobiush5');
    conn.release();
  })
  .catch(err => {
    console.error('❌ Error MySQL:', err.message);
  });

module.exports = pool;
