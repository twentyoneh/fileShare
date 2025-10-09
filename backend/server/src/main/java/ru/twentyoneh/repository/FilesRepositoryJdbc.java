package ru.twentyoneh.repository;

import ru.twentyoneh.dto.FileRecord;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FilesRepositoryJdbc implements FilesRepository {
    private final DataSource ds;
    public FilesRepositoryJdbc(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public void insert(FileRecord rec) {
        String sql = """
                INSERT INTO files (id, original_name, stored_key, size_bytes,
                mime_type, sha256, token, created_at, last_download_at, download_count)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try(var conn = ds.getConnection(); var ps = conn.prepareStatement(sql)) {
            ps.setObject(1,rec.id());
            ps.setString(2,rec.originalName());
            ps.setString(3,rec.storedKey());
            ps.setLong(4,rec.sizeBytes());
            setStrOrNull(ps,5,rec.mimeType());
            setStrOrNull(ps,6,rec.sha256());
            ps.setString(7, rec.token());
            ps.setTimestamp(8, Timestamp.from(rec.createdAt()));
            setTsOrNull(ps,9,rec.lastDownloadAt());
            ps.setInt(10, rec.downloadCount());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("insert failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<FileRecord> findByToken(String token) {
        String sql = """
                SELECT id, original_name, stored_key, size_bytes, mime_type, sha256, token,
                created_at, last_download_at, download_count
                FROM files WHERE token=?
                """;
        try(var conn = ds.getConnection(); var ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (var rs = ps.executeQuery()) {
                if(rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("findByToken failed: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @Override
    public long countAll() {
        try (var c = ds.getConnection();
             var st = c.createStatement();
             var rs = st.executeQuery("select count(*) from files")) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println("countAll failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FileRecord> list(int limit, int offset) {
        String sql = """
      SELECT id, original_name, stored_key, size_bytes, mime_type, sha256, token,
             created_at, last_download_at, download_count
      FROM files ORDER BY created_at desc LIMIT ? OFFSET ?
      """;
        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            List<FileRecord> out = new ArrayList<>();
            try (var rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
            return out;
        } catch (SQLException e) {
            System.err.println("list failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void incDownloadAndTouch(UUID id) {
        String sql = "UPDATE files SET download_count = download_count + 1, last_download_at = now() WHERE id = ?";
        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("incDownload failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FileRecord> selectExpired(int retentionDays, int limit) {
        // Разница между сейчас и последним скачиванием || созданием файла >= чем retentionDays
        String sql = """ 
      SELECT id, original_name, stored_key, size_bytes, mime_type, sha256, token,
             created_at, last_download_at, download_count
      FROM files
      WHERE (now() - COALESCE(last_download_at, created_at)) >= make_interval(days => ?) 
      ORDER BY created_at
      LIMIT ?
      """;
        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, retentionDays);
            ps.setInt(2, limit);
            var out = new java.util.ArrayList<FileRecord>();
            try (var rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
            return out; // вывод списка удалённых эл-ов
        } catch (Exception e) {
            System.err.println("selectExpired failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(UUID id) {
        try (var c = ds.getConnection(); var ps = c.prepareStatement("DELETE FROM files WHERE id = ?")) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("deleteById failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static FileRecord map(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        String originalName = rs.getString("original_name");
        String storedKey = rs.getString("stored_key");
        long sizeBytes = rs.getLong("size_bytes");
        String mime = rs.getString("mime_type"); if (rs.wasNull()) mime=null;
        String sha = rs.getString("sha256");     if (rs.wasNull()) sha=null;
        String token = rs.getString("token");
        Instant created = rs.getTimestamp("created_at").toInstant();
        Timestamp ldt = rs.getTimestamp("last_download_at");
        Instant last = ldt==null? null : ldt.toInstant();
        int cnt = rs.getInt("download_count");
        return new FileRecord(id, originalName, storedKey, sizeBytes, mime, sha, token, created, last, cnt);
    }
    private static void setStrOrNull(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v==null) ps.setNull(idx, Types.VARCHAR); else ps.setString(idx, v);
    }
    private static void setTsOrNull(PreparedStatement ps, int idx, Instant v) throws SQLException {
        if (v==null) ps.setNull(idx, Types.TIMESTAMP_WITH_TIMEZONE); else ps.setTimestamp(idx, Timestamp.from(v));
    }
}
