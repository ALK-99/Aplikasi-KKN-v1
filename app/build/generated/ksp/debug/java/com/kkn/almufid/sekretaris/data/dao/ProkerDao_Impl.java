package com.kkn.almufid.sekretaris.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.kkn.almufid.sekretaris.data.Converters;
import com.kkn.almufid.sekretaris.data.entity.ProkerEntry;
import com.kkn.almufid.sekretaris.data.entity.StatusProker;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ProkerDao_Impl implements ProkerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ProkerEntry> __insertionAdapterOfProkerEntry;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<ProkerEntry> __deletionAdapterOfProkerEntry;

  private final EntityDeletionOrUpdateAdapter<ProkerEntry> __updateAdapterOfProkerEntry;

  public ProkerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfProkerEntry = new EntityInsertionAdapter<ProkerEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `proker_entries` (`id`,`namaProker`,`divisiPenanggungJawab`,`status`,`catatanEvaluasi`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProkerEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getNamaProker());
        statement.bindString(3, entity.getDivisiPenanggungJawab());
        final String _tmp = __converters.statusProkerKeString(entity.getStatus());
        statement.bindString(4, _tmp);
        statement.bindString(5, entity.getCatatanEvaluasi());
      }
    };
    this.__deletionAdapterOfProkerEntry = new EntityDeletionOrUpdateAdapter<ProkerEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `proker_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProkerEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfProkerEntry = new EntityDeletionOrUpdateAdapter<ProkerEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `proker_entries` SET `id` = ?,`namaProker` = ?,`divisiPenanggungJawab` = ?,`status` = ?,`catatanEvaluasi` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProkerEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getNamaProker());
        statement.bindString(3, entity.getDivisiPenanggungJawab());
        final String _tmp = __converters.statusProkerKeString(entity.getStatus());
        statement.bindString(4, _tmp);
        statement.bindString(5, entity.getCatatanEvaluasi());
        statement.bindLong(6, entity.getId());
      }
    };
  }

  @Override
  public Object tambah(final ProkerEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfProkerEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object hapus(final ProkerEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfProkerEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object ubah(final ProkerEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfProkerEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ProkerEntry>> semuaProker() {
    final String _sql = "SELECT * FROM proker_entries ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"proker_entries"}, new Callable<List<ProkerEntry>>() {
      @Override
      @NonNull
      public List<ProkerEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNamaProker = CursorUtil.getColumnIndexOrThrow(_cursor, "namaProker");
          final int _cursorIndexOfDivisiPenanggungJawab = CursorUtil.getColumnIndexOrThrow(_cursor, "divisiPenanggungJawab");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCatatanEvaluasi = CursorUtil.getColumnIndexOrThrow(_cursor, "catatanEvaluasi");
          final List<ProkerEntry> _result = new ArrayList<ProkerEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ProkerEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpNamaProker;
            _tmpNamaProker = _cursor.getString(_cursorIndexOfNamaProker);
            final String _tmpDivisiPenanggungJawab;
            _tmpDivisiPenanggungJawab = _cursor.getString(_cursorIndexOfDivisiPenanggungJawab);
            final StatusProker _tmpStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.stringKeStatusProker(_tmp);
            final String _tmpCatatanEvaluasi;
            _tmpCatatanEvaluasi = _cursor.getString(_cursorIndexOfCatatanEvaluasi);
            _item = new ProkerEntry(_tmpId,_tmpNamaProker,_tmpDivisiPenanggungJawab,_tmpStatus,_tmpCatatanEvaluasi);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
