/**
 * ============================================================================
 * MÓDULO DE BASE DE DATOS - SQLite para Persistencia de Chat
 * ============================================================================
 *
 * Maneja el almacenamiento de mensajes de chat en SQLite
 *
 * ============================================================================
 */

const Database = require('better-sqlite3');
const path = require('path');

// Crear/abrir base de datos
const db = new Database(path.join(__dirname, 'streaming.db'));

// Habilitar WAL mode para mejor rendimiento en concurrencia
db.pragma('journal_mode = WAL');

/**
 * Inicializar la base de datos y crear tablas
 */
function initDatabase() {
    // Tabla de mensajes de chat
    const createChatMessagesTable = db.prepare(`
        CREATE TABLE IF NOT EXISTS chat_messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            message_id TEXT NOT NULL,
            stream_key TEXT NOT NULL,
            username TEXT NOT NULL,
            message TEXT NOT NULL,
            timestamp INTEGER NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    `);

    createChatMessagesTable.run();

    // Índices para mejorar rendimiento de consultas
    db.prepare(`
        CREATE INDEX IF NOT EXISTS idx_stream_key
        ON chat_messages(stream_key)
    `).run();

    db.prepare(`
        CREATE INDEX IF NOT EXISTS idx_timestamp
        ON chat_messages(timestamp)
    `).run();

    console.log('[Database] Base de datos SQLite inicializada');
}

/**
 * Guardar un mensaje de chat
 * @param {Object} messageData - {messageId, streamKey, username, message, timestamp}
 */
function saveChatMessage(messageData) {
    const stmt = db.prepare(`
        INSERT INTO chat_messages (message_id, stream_key, username, message, timestamp)
        VALUES (?, ?, ?, ?, ?)
    `);

    try {
        stmt.run(
            messageData.messageId,
            messageData.streamKey,
            messageData.username,
            messageData.message,
            messageData.timestamp
        );
        return true;
    } catch (error) {
        console.error('[Database] Error guardando mensaje:', error);
        return false;
    }
}

/**
 * Obtener historial de chat de un stream
 * @param {String} streamKey - Clave del stream
 * @param {Object} options - {limit, offset, startDate, endDate}
 * @returns {Array} Array de mensajes
 */
function getChatHistory(streamKey, options = {}) {
    const { limit = 1000, offset = 0, startDate, endDate } = options;

    let query = `
        SELECT message_id, stream_key, username, message, timestamp, created_at
        FROM chat_messages
        WHERE stream_key = ?
    `;

    const params = [streamKey];

    // Filtro por rango de fechas si se proporciona
    if (startDate) {
        query += ` AND timestamp >= ?`;
        params.push(startDate);
    }

    if (endDate) {
        query += ` AND timestamp <= ?`;
        params.push(endDate);
    }

    query += ` ORDER BY timestamp ASC LIMIT ? OFFSET ?`;
    params.push(limit, offset);

    const stmt = db.prepare(query);
    return stmt.all(...params);
}

/**
 * Obtener estadísticas de mensajes por stream
 * @param {String} streamKey - Clave del stream
 * @returns {Object} Estadísticas {totalMessages, firstMessage, lastMessage}
 */
function getChatStats(streamKey) {
    const stmt = db.prepare(`
        SELECT
            COUNT(*) as total_messages,
            MIN(timestamp) as first_message,
            MAX(timestamp) as last_message,
            COUNT(DISTINCT username) as unique_users
        FROM chat_messages
        WHERE stream_key = ?
    `);

    return stmt.get(streamKey);
}

/**
 * Obtener todos los streams que tienen mensajes
 * @returns {Array} Array de stream keys con estadísticas
 */
function getAllStreamsWithChat() {
    const stmt = db.prepare(`
        SELECT
            stream_key,
            COUNT(*) as message_count,
            MIN(timestamp) as first_message,
            MAX(timestamp) as last_message,
            COUNT(DISTINCT username) as unique_users
        FROM chat_messages
        GROUP BY stream_key
        ORDER BY last_message DESC
    `);

    return stmt.all();
}

/**
 * Eliminar mensajes antiguos (mantenimiento)
 * @param {Number} daysToKeep - Días de historial a mantener
 * @returns {Number} Cantidad de mensajes eliminados
 */
function cleanOldMessages(daysToKeep = 30) {
    const cutoffTimestamp = Date.now() - (daysToKeep * 24 * 60 * 60 * 1000);

    const stmt = db.prepare(`
        DELETE FROM chat_messages
        WHERE timestamp < ?
    `);

    const result = stmt.run(cutoffTimestamp);
    return result.changes;
}

/**
 * Buscar mensajes por palabra clave
 * @param {String} streamKey - Clave del stream
 * @param {String} keyword - Palabra a buscar
 * @returns {Array} Array de mensajes que contienen la palabra
 */
function searchMessages(streamKey, keyword) {
    const stmt = db.prepare(`
        SELECT message_id, stream_key, username, message, timestamp
        FROM chat_messages
        WHERE stream_key = ? AND message LIKE ?
        ORDER BY timestamp DESC
        LIMIT 100
    `);

    return stmt.all(streamKey, `%${keyword}%`);
}

/**
 * Cerrar conexión a la base de datos
 */
function closeDatabase() {
    db.close();
    console.log('[Database] Conexión cerrada');
}

// Inicializar base de datos al cargar el módulo
initDatabase();

// Exportar funciones
module.exports = {
    saveChatMessage,
    getChatHistory,
    getChatStats,
    getAllStreamsWithChat,
    cleanOldMessages,
    searchMessages,
    closeDatabase,
    db
};
