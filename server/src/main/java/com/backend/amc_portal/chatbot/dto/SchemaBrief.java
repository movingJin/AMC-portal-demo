package com.backend.amc_portal.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SchemaBrief(
    List<TableBrief> tables,
    List<JoinCandidate> joinCandidates,
    List<ValueAnchor> valueAnchors,
    String notes) {
  public SchemaBrief {
    tables = tables != null ? tables : List.of();
    joinCandidates = joinCandidates != null ? joinCandidates : List.of();
    valueAnchors = valueAnchors != null ? valueAnchors : List.of();
    notes = notes != null ? notes : "";
  }

  public static SchemaBrief empty() {
    return new SchemaBrief(List.of(), List.of(), List.of(), "");
  }

  public record TableBrief(
      String name,
      String kind,
      String description,
      List<String> pk,
      List<ColumnBrief> columns,
      List<ForeignKey> foreignKeys,
      List<SampleHint> sampleHints) {}

  public record ColumnBrief(String name, String type, Boolean nullable, String desc, String tag) {}

  public record ForeignKey(String from, String to) {}

  public record SampleHint(String col, List<Object> values) {}

  public record JoinCandidate(String from, String to, String via) {}

  public record ValueAnchor(
      String value,
      boolean found,
      List<Location> locations,
      List<RuledOut> ruledOut,
      String note) {}

  public record Location(String table, String column, String sample) {}

  public record RuledOut(String table, String column, String reason) {}

  /** 메인이 여러 schema-explorer 결과를 병합. */
  public SchemaBrief mergeWith(SchemaBrief other) {
    if (other == null) return this;

    Map<String, TableBrief> tableMap = new LinkedHashMap<>();
    for (TableBrief t : tables) tableMap.put(t.name(), t);
    for (TableBrief t : other.tables()) tableMap.putIfAbsent(t.name(), t);

    List<JoinCandidate> joins = new ArrayList<>(joinCandidates);
    for (JoinCandidate j : other.joinCandidates()) {
      boolean dup =
          joins.stream()
              .anyMatch(x -> eq(x.from(), j.from()) && eq(x.to(), j.to()) && eq(x.via(), j.via()));
      if (!dup) joins.add(j);
    }

    // value_anchors: 같은 value 끼리 locations / ruled_out 합집합
    Map<String, ValueAnchor> anchorMap = new LinkedHashMap<>();
    for (ValueAnchor v : valueAnchors) anchorMap.put(v.value(), v);
    for (ValueAnchor v : other.valueAnchors()) {
      ValueAnchor existing = anchorMap.get(v.value());
      if (existing == null) {
        anchorMap.put(v.value(), v);
      } else {
        List<Location> locs = mergeList(existing.locations(), v.locations());
        List<RuledOut> ruled = mergeList(existing.ruledOut(), v.ruledOut());
        boolean found = existing.found() || v.found();
        String note =
            (existing.note() == null || existing.note().isBlank()) ? v.note() : existing.note();
        anchorMap.put(v.value(), new ValueAnchor(v.value(), found, locs, ruled, note));
      }
    }

    String mergedNotes = (notes + "\n" + Optional.ofNullable(other.notes()).orElse("")).trim();
    return new SchemaBrief(
        new ArrayList<>(tableMap.values()),
        joins,
        new ArrayList<>(anchorMap.values()),
        mergedNotes);
  }

  private static <T> List<T> mergeList(List<T> a, List<T> b) {
    List<T> out = new ArrayList<>(a == null ? List.of() : a);
    if (b != null) for (T x : b) if (!out.contains(x)) out.add(x);
    return out;
  }

  private static boolean eq(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }

  /** 컬럼이 schema에 존재하는지 빠른 조회. 입력 형식: "schema.table.column" 또는 "table.column". */
  public boolean hasColumn(String fqColumn) {
    String[] parts = fqColumn.split("\\.");
    if (parts.length < 2) return false;
    String col = parts[parts.length - 1];
    String tableName = parts[parts.length - 2];
    String schemaName = parts.length >= 3 ? parts[parts.length - 3] : null;

    for (TableBrief t : tables) {
      String[] tn = t.name().split("\\.");
      String tTable = tn[tn.length - 1];
      String tSchema = tn.length >= 2 ? tn[tn.length - 2] : null;
      if (!tTable.equalsIgnoreCase(tableName)) continue;
      if (schemaName != null && tSchema != null && !tSchema.equalsIgnoreCase(schemaName)) continue;
      for (ColumnBrief c : t.columns()) {
        if (c.name().equalsIgnoreCase(col)) return true;
      }
    }
    return false;
  }

  public Optional<ValueAnchor> findAnchor(String value) {
    return valueAnchors.stream().filter(v -> v.value().equals(value) && v.found()).findFirst();
  }
}
