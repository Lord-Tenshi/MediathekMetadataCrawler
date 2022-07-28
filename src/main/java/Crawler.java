import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class Crawler {
    final String URL = "https://mediathek.htw-berlin.de/";
    final String CATEGORY = "category";
    final String AWE = "/awe/87";
    final String CONSTRUCTION_ENG = "/bauingenieurwesen/40";
    final String CHMEMISTRY = "/chemie/88";
    final String DESIGN = "/design/41";
    final String ELECTR_ENG = "/elektrotechnik-elektronik-nachrichtentechnik/42";
    final String ENERGY_TECH = "/energietechnik-umwelttechnik/60";
    final String IT = "/informatik/44";

    public static void main(String[] args) {
        Crawler crawler = new Crawler();
        Document aweTabDoc = crawler.tryGetDocPage(crawler.CATEGORY + crawler.AWE);
        Document itTabDoc = crawler.tryGetDocPage(crawler.CATEGORY + crawler.IT);
        Document chemTabDoc = crawler.tryGetDocPage(crawler.CATEGORY + crawler.CHMEMISTRY);
        Document constTabDoc = crawler.tryGetDocPage(crawler.CATEGORY + crawler.CONSTRUCTION_ENG);
        Document elecTabDoc = crawler.tryGetDocPage(crawler.CATEGORY + crawler.ELECTR_ENG);
        Document designTabDoc = crawler.tryGetDocPage(crawler.CATEGORY + crawler.DESIGN);
        Document energyTabDoc = crawler.tryGetDocPage(crawler.CATEGORY + crawler.ENERGY_TECH);

        List<String> titles = crawler.readOutVideoTitles(aweTabDoc);
        titles.addAll(crawler.readOutVideoTitles(itTabDoc));
        titles.addAll(crawler.readOutVideoTitles(chemTabDoc));
        titles.addAll(crawler.readOutVideoTitles(constTabDoc));
        titles.addAll(crawler.readOutVideoTitles(elecTabDoc));
        titles.addAll(crawler.readOutVideoTitles(designTabDoc));
        titles.addAll(crawler.readOutVideoTitles(energyTabDoc));

        ArrayList<String> usedTitles = new ArrayList<>();

        List<Medium> media = new ArrayList<>();
        for (String title : titles) {
            Document videoPlayerDoc = crawler.tryGetDocPage(title);
            Medium m = crawler.getVideoMetadata(videoPlayerDoc);
            if(m!=null) {
                media.add(m);
            }
        }
        List<Medium> mediaDistinct = media.stream().distinct().collect(Collectors.toList());
        //crawler.printJsonAllMetadata(mediaDistinct);
        crawler.printJsonTuples(mediaDistinct, usedTitles);
    }

    // prints JSON tuples with each category with title to file
    public void printJsonTuples(List<Medium> media, List<String> usedTitles) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ "); // Open Json
        for (int i = 0; i < media.size(); i++) {
            Medium m = media.get(i);
            String title = m.getTitle();

            //prevent duplicates across multiple categories
            if(usedTitles.contains(title)){
                continue;
            }
            usedTitles.add(title);
            for (String cat : m.categories) {
                sb.append("{\n");
                sb.append(
                        String.format("\"title\" : \"%s\",\n", title));
                sb.append(
                        String.format("\"category\" : \"%s\"\n", cat));
                sb.append("}");
                if (!(i == media.size() - 1)) {
                    sb.append(", ");
                }
            }

        }
        sb.append(" ]");
        try {
            writeMediaToJsonFileCustom(sb.toString());
        } catch (IOException e) {
            System.out.println("Writing media to custom json format failed\n");
            e.printStackTrace();
        }
    }

    public void printJsonAllMetadata(List<Medium> media) {
        try {
            writeMediaToJsonFile(media);
        } catch (Exception e) {
            System.out.println("Writing media to json failed\n");
            e.printStackTrace();
        }
    }

    public void writeMediaToJsonFileCustom(String stringToWrite) throws IOException {
        FileWriter writer = new FileWriter("crawledDataCustom.json", false);

        writer.write(stringToWrite);
        writer.close();
    }

    public void writeMediaToJsonFile(List<Medium> media) throws IOException {
        ObjectMapper mapper = new JsonMapper();
        FileWriter writer = new FileWriter("crawledData.json", false);

        String jsonMedium = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(media);
        writer.write(jsonMedium);
        writer.close();
    }

    private Document tryGetDocPage(String subPage) {
        int retryCount = 1;
        Document doc;
        while (retryCount < 5) {
            try {
                doc = Jsoup.connect(URL + subPage).get();
                return doc;
            } catch (IOException io) {
                System.out.println("Failed to get document page\nAttempt:" + retryCount);
                retryCount++;
            }
        }
        System.exit(-1);
        return null;
    }

    public Medium getVideoMetadata(Document doc) {
        Elements titleLink = doc.getElementsByTag("h1");
        Elements tags = doc.getElementsByClass("tag");
        Element categories = doc.getElementsByClass("categories").first();
        if(categories == null){
            return null; // exit metadata extraction, because the Video is unusable
        }
        List<String> strCategories = categories.children().eachText();

        // remove faulty category found
        strCategories.remove(0);

        Elements description = doc.getElementsByClass("description");

        return Medium.builder()
                .title(titleLink.text())
                .description(description.text())
                .tags(tags.eachText())
                .categories(strCategories)
                .build();
    }

    public List<String> readOutVideoTitles(Document doc) {
        Elements titleLinksWithDuplicates = doc.getElementsByAttributeValueStarting("href", "/category/");
        return titleLinksWithDuplicates.eachAttr("href")
                .stream()
                .distinct()
                .collect(Collectors.toList());
    }
}
