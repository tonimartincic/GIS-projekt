import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private final static String url = "jdbc:postgresql://localhost:5432/gis-projekt";
    private final static String user = "postgres";
    private final static String password = ""; // TODO: add your password here

    /**
     * Connect to the PostgreSQL database
     *
     * @return a Connection object
     * @throws java.sql.SQLException
     */
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static void main(String[] args) {
        Instant start = Instant.now();

        try (Connection conn = connect()) {
            List<Line> dataLines = new LinkedList<>();

            LocalDateTime date = LocalDateTime.of(
                    LocalDate.of(2018, Month.FEBRUARY, 1),
                    LocalTime.of(18, 30)
            );

            int i = 0;
            while(true) {
                i++;
                if (i % 1000 == 0) {
                    System.out.println("ITERACIJA: " + i + " POSTOTAK: " + (dataLines.size() * 1.0 / 14000) * 100 + "%");
                }

                if (dataLines.size() > 14000) {
                    System.out.println(dataLines.size() + " redaka nađeno u " + i + " iteracija");
                    break;
                }
                LocalDateTime date2 = date.plusMinutes(10);
                LocalDateTime date3 = date2.plusMinutes(10);
                LocalDateTime date4 = date3.plusMinutes(10);

                String timestamp3 = date.toString();
                String timestamp2 = date2.toString();
                String timestamp1 = date3.toString();
                String timestamp0 = date4.toString();

                String SQL = "select time_id, relation_id, cluster_id, \n" +
                        "ST_X(centroid) as X, ST_Y(centroid) as Y, ST_AsText(polygon) as polygon, stroke_count, ST_Area(polygon) as area from (\n" +
                        "\tselect * from nowcast.find_cluster_relations(\n" +
                        "\t\t$$ \n" +
                        "\t\tselect -3 as time_id, *\n" +
                        "\t\t\tfrom nowcast.compute_clusters_from_facts(timestamp '" + date.toString() + "', interval '10 min', 0.1, 10)\n" +
                        "\t\tunion all\n" +
                        "\t\tselect -2 as time_id, *\n" +
                        "\t\t\tfrom nowcast.compute_clusters_from_facts(timestamp '" + date2.toString() + "', interval '10 min', 0.1, 10)\n" +
                        "\t\tunion all\n" +
                        "\t\tselect -1 as time_id, *\n" +
                        "\t\t\tfrom nowcast.compute_clusters_from_facts(timestamp '" + date3.toString() + "', interval '10 min', 0.1, 10)\n" +
                        "\t\tunion all\n" +
                        "\t\tselect 0 as time_id, *\n" +
                        "\t\t\tfrom nowcast.compute_clusters_from_facts(timestamp '" + date4.toString() + "', interval '10 min', 0.1, 10)\n" +
                        "\t\t$$, \n" +
                        "\t\t0.05\n" +
                        "\t)) as innerresult;";
                try (PreparedStatement pstmt = conn.prepareStatement(SQL)) {

                    ResultSet rs = pstmt.executeQuery();

                    Map<Integer, Map<Integer, NTorka>> relacijaClusterNtorka = new HashMap<>();
                    while (rs.next()) {
                        int relation_id = rs.getInt("relation_id");
                        int time_id = rs.getInt("time_id");
                        int cluster_id = rs.getInt("cluster_id");
                        double x = rs.getDouble("x");
                        double y = rs.getDouble("y");
                        String polygon = rs.getString("polygon");
                        int stroke_count = rs.getInt("stroke_count");
                        double area = rs.getDouble("area");

                        Map<Integer, NTorka> clusterNtorka = null;
                        if (relacijaClusterNtorka.containsKey(relation_id)) {
                            clusterNtorka = relacijaClusterNtorka.get(relation_id);
                        } else {
                            clusterNtorka = new HashMap<>();
                            relacijaClusterNtorka.put(relation_id, clusterNtorka);
                        }

                        NTorka value = null;
                        if (clusterNtorka.containsKey(cluster_id)) {
                            value = clusterNtorka.get(cluster_id);
                        } else {
                            value = new NTorka();
                            clusterNtorka.put(cluster_id, value);
                        }

                        Line line = new Line(
                            i,
                            relation_id,
                            time_id,
                            Collections.singletonList(cluster_id),
                            x,
                            y,
                            Collections.singletonList(polygon),
                            Collections.singletonList(stroke_count),
                            area,
                            "");

                        if (time_id == 0) {
                            line.setTimestamp(timestamp0);
                            value.setLine0(line);
                        } else if (time_id == -1) {
                            line.setTimestamp(timestamp1);
                            value.setLineMinus1(line);
                        } else if (time_id == -2) {
                            line.setTimestamp(timestamp2);
                            value.setLineMinus2(line);
                        } else if (time_id == -3) {
                            line.setTimestamp(timestamp3);
                            value.setLineMinus3(line);
                        }
                    }

                    for (Integer relationId : relacijaClusterNtorka.keySet()) {
                        if (relacijaClusterNtorka.get(relationId).keySet().size() == 1) {
                            for (Integer clusterId : relacijaClusterNtorka.get(relationId).keySet()) {
                                NTorka value = relacijaClusterNtorka.get(relationId).get(clusterId);
                                if (value.getLineMinus3() != null
                                    && value.getLineMinus2() != null
                                    && value.getLineMinus1() != null
                                    && value.getLine0() != null) {
                                        dataLines.add(value.getLineMinus3());
                                        dataLines.add(value.getLineMinus2());
                                        dataLines.add(value.getLineMinus1());
                                        dataLines.add(value.getLine0());
                                }
                            }
                        } else {
                            double xT3 = 0;
                            double yT3 = 0;
                            double xT2 = 0;
                            double yT2 = 0;
                            double xT1 = 0;
                            double yT1 = 0;
                            double xT0 = 0;
                            double yT0 = 0;

                            int strokesSumT3 = 0;
                            int strokesSumT2 = 0;
                            int strokesSumT1 = 0;
                            int strokesSumT0 = 0;

                            List<Integer> clusterIds3 = new LinkedList<>();
                            List<String> polygons3 = new LinkedList<>();
                            List<Integer> strokeCounts3 = new LinkedList<>();

                            List<Integer> clusterIds2 = new LinkedList<>();
                            List<String> polygons2 = new LinkedList<>();
                            List<Integer> strokeCounts2 = new LinkedList<>();

                            List<Integer> clusterIds1 = new LinkedList<>();
                            List<String> polygons1 = new LinkedList<>();
                            List<Integer> strokeCounts1 = new LinkedList<>();

                            List<Integer> clusterIds0 = new LinkedList<>();
                            List<String> polygons0 = new LinkedList<>();
                            List<Integer> strokeCounts0 = new LinkedList<>();

                            double area3 = 0;
                            double area2 = 0;
                            double area1 = 0;
                            double area0 = 0;

                            boolean sadrziBaremJednu = false;
                            for (Integer clusterId : relacijaClusterNtorka.get(relationId).keySet()) {
                                NTorka value = relacijaClusterNtorka.get(relationId).get(clusterId);
                                if (value.getLine0() == null
                                        || value.getLineMinus1() == null
                                        || value.getLineMinus2() == null
                                        || value.getLineMinus3() == null) {
                                    continue;
                                }

                                sadrziBaremJednu = true;

                                xT3 += value.getLineMinus3().getX() * value.getLineMinus3().getStrokeCounts().get(0);
                                yT3 += value.getLineMinus3().getY() * value.getLineMinus3().getStrokeCounts().get(0);
                                strokesSumT3 += value.getLineMinus3().getStrokeCounts().get(0);

                                xT2 += value.getLineMinus2().getX() * value.getLineMinus2().getStrokeCounts().get(0);
                                yT2 += value.getLineMinus2().getY() * value.getLineMinus2().getStrokeCounts().get(0);
                                strokesSumT2 += value.getLineMinus2().getStrokeCounts().get(0);

                                xT1 += value.getLineMinus1().getX() * value.getLineMinus1().getStrokeCounts().get(0);
                                yT1 += value.getLineMinus1().getY() * value.getLineMinus1().getStrokeCounts().get(0);
                                strokesSumT1 += value.getLineMinus1().getStrokeCounts().get(0);

                                xT0 += value.getLine0().getX() * value.getLine0().getStrokeCounts().get(0);
                                yT0 += value.getLine0().getY() * value.getLine0().getStrokeCounts().get(0);
                                strokesSumT0 += value.getLine0().getStrokeCounts().get(0);

                                clusterIds3.add(value.getLineMinus3().getClusterIds().get(0));
                                polygons3.add(value.getLineMinus3().getPolygons().get(0));
                                strokeCounts3.add(value.getLineMinus3().getStrokeCounts().get(0));

                                clusterIds2.add(value.getLineMinus2().getClusterIds().get(0));
                                polygons2.add(value.getLineMinus2().getPolygons().get(0));
                                strokeCounts2.add(value.getLineMinus2().getStrokeCounts().get(0));

                                clusterIds1.add(value.getLineMinus1().getClusterIds().get(0));
                                polygons1.add(value.getLineMinus1().getPolygons().get(0));
                                strokeCounts1.add(value.getLineMinus1().getStrokeCounts().get(0));

                                clusterIds0.add(value.getLine0().getClusterIds().get(0));
                                polygons0.add(value.getLine0().getPolygons().get(0));
                                strokeCounts0.add(value.getLine0().getStrokeCounts().get(0));

                                area3 += value.getLineMinus3().getUkupnaPovrsina();
                                area2 += value.getLineMinus2().getUkupnaPovrsina();
                                area1 += value.getLineMinus1().getUkupnaPovrsina();
                                area0 += value.getLine0().getUkupnaPovrsina();
                            }

                            if (sadrziBaremJednu) {
                                xT3 /= strokesSumT3;
                                yT3 /= strokesSumT3;

                                xT2 /= strokesSumT2;
                                yT2 /= strokesSumT2;

                                xT1 /= strokesSumT1;
                                yT1 /= strokesSumT1;

                                xT0 /= strokesSumT0;
                                yT0 /= strokesSumT0;

                                Line line3 = new Line(
                                        i,
                                        relationId,
                                        -3,
                                        clusterIds3,
                                        xT3,
                                        yT3,
                                        polygons3,
                                        strokeCounts3,
                                        area3,
                                        timestamp3);

                                Line line2 = new Line(
                                        i,
                                        relationId,
                                        -2,
                                        clusterIds2,
                                        xT2,
                                        yT2,
                                        polygons2,
                                        strokeCounts2,
                                        area2,
                                        timestamp2);

                                Line line1 = new Line(
                                        i,
                                        relationId,
                                        -1,
                                        clusterIds1,
                                        xT1,
                                        yT1,
                                        polygons1,
                                        strokeCounts1,
                                        area1,
                                        timestamp1);

                                Line line0 = new Line(
                                        i,
                                        relationId,
                                        0,
                                        clusterIds0,
                                        xT0,
                                        yT0,
                                        polygons0,
                                        strokeCounts0,
                                        area0,
                                        timestamp0);

                                dataLines.add(line3);
                                dataLines.add(line2);
                                dataLines.add(line1);
                                dataLines.add(line0);
                            }
                        }
                    }
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }

                date = date.plusMinutes(10);
            }

            File csvOutputFile = new File("podaci-za-ucenje.csv");
            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                String[] firstLine = new String[]{"ID (ITERACIJA--relation)", "relation id", "time_id", "cluster ids", "x", "y", "polygons", "stroke counts", "area", "timestamp (milis)"};
                pw.println(convertToCSV(firstLine));
                dataLines.stream()
                        .map(Main::convertToCSV)
                        .forEach(pw::println);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis() / 1000;
        System.out.println("Izvodilo se " + timeElapsed + " sekundi");
    }

    public static String convertToCSV(Line line) {
        String[] data = new String[] {
                line.getIteracija() + "--" + line.getRelationId(),
                String.valueOf(line.getRelationId()),
                String.valueOf(line.getTimeId()),
                line.getClusterIds()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" ")),
                String.valueOf(line.getX()),
                String.valueOf(line.getY()),
                String.join(" ", line.getPolygons()),
                line.getStrokeCounts()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" ")),
                String.valueOf(line.getUkupnaPovrsina()),
                String.valueOf(line.getTimestamp())};

        return Stream.of(data)
                .map(x -> escapeSpecialCharacters(x))
                .collect(Collectors.joining(","));
    }

    public static String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(x -> escapeSpecialCharacters(x))
                .collect(Collectors.joining(","));
    }

    public static String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    public static class NTorka {
        private Line lineMinus3;
        private Line lineMinus2;
        private Line lineMinus1;
        private Line line0;

        public Line getLineMinus3() {
            return lineMinus3;
        }

        public void setLineMinus3(Line lineMinus3) {
            this.lineMinus3 = lineMinus3;
        }

        public Line getLineMinus2() {
            return lineMinus2;
        }

        public void setLineMinus2(Line lineMinus2) {
            this.lineMinus2 = lineMinus2;
        }

        public Line getLineMinus1() {
            return lineMinus1;
        }

        public void setLineMinus1(Line lineMinus1) {
            this.lineMinus1 = lineMinus1;
        }

        public Line getLine0() {
            return line0;
        }

        public void setLine0(Line line0) {
            this.line0 = line0;
        }
    }

    public static class Line {
        private int iteracija;
        private int relationId;
        private int timeId;
        private List<Integer> clusterIds;
        private double x;
        private double y;
        private List<String> polygons;
        private List<Integer> strokeCounts;
        private double ukupnaPovrsina;
        private String timestamp;

        public Line(
                int iteracija,
                int relationId,
                int timeId,
                List<Integer> clusterIds,
                double x,
                double y,
                List<String> polygons,
                List<Integer> strokeCounts,
                double ukupnaPovrsina,
                String timestamp) {
            this.iteracija = iteracija;
            this.relationId = relationId;
            this.timeId = timeId;
            this.clusterIds = clusterIds;
            this.x = x;
            this.y = y;
            this.polygons = polygons;
            this.strokeCounts = strokeCounts;
            this.ukupnaPovrsina = ukupnaPovrsina;
            this.timestamp = timestamp;
        }

        public int getIteracija() {
            return iteracija;
        }

        public void setIteracija(int iteracija) {
            this.iteracija = iteracija;
        }

        public int getRelationId() {
            return relationId;
        }

        public void setRelationId(int relationId) {
            this.relationId = relationId;
        }

        public int getTimeId() {
            return timeId;
        }

        public void setTimeId(int timeId) {
            this.timeId = timeId;
        }

        public List<Integer> getClusterIds() {
            return clusterIds;
        }

        public void setClusterIds(List<Integer> clusterIds) {
            this.clusterIds = clusterIds;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public List<String> getPolygons() {
            return polygons;
        }

        public void setPolygons(List<String> polygons) {
            this.polygons = polygons;
        }

        public List<Integer> getStrokeCounts() {
            return strokeCounts;
        }

        public void setStrokeCounts(List<Integer> strokeCounts) {
            this.strokeCounts = strokeCounts;
        }

        public double getUkupnaPovrsina() {
            return ukupnaPovrsina;
        }

        public void setUkupnaPovrsina(double ukupnaPovrsina) {
            this.ukupnaPovrsina = ukupnaPovrsina;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }
}
