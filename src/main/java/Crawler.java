import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class Crawler {
    final String URL = "https://mediathek.htw-berlin.de/";
    final String VIDEOS = "videos";

    public static void main(String[] args) {
        Crawler crawler = new Crawler();
        Document videoTabDoc = crawler.tryGetDocPage(crawler.VIDEOS);
        List<String> titles = crawler.readOutVideoTitles(videoTabDoc);
        List<Medium> media = new ArrayList<>();
        for (String title : titles) {
            Document videoPlayerDoc = crawler.tryGetDocPage(title);
            media.add(crawler.getVideoMetadata(videoPlayerDoc));
        }
        try {
            crawler.writeMediaToJsonFile(media);
        } catch (Exception e) {
            System.out.println("Writing media to json failed\n");
            e.printStackTrace();
        }
    }

    public void writeMediaToJsonFile(List<Medium> media) throws IOException {
        ObjectMapper mapper = new JsonMapper();
        FileWriter writer = new FileWriter("crawledData.json");

        String jsonMedium = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(media);
        writer.write(jsonMedium);
        writer.close();
    }

    private Document tryGetDocPage(String subPage) {
        int retryCount = 1;
        Document doc;
        while(retryCount<5){
            try {
                doc = Jsoup.connect(URL + subPage).get();
                return doc;
            }catch (IOException io){
                System.out.println("Failed to get document page\nAttempt:"+ retryCount);
                retryCount++;
            }
        }
        System.exit(-1);
        return null;
    }

    public Medium getVideoMetadata(Document doc) {
        Elements titleLink = doc.getElementsByTag("h1");
        Elements tags = doc.getElementsByClass("tag");
        Elements categories = doc.getElementsByClass("categories");
        Elements description = doc.getElementsByClass("description");

        return new Medium().builder()
                .title(titleLink.text())
                .description(description.text())
                .tags(tags.eachText())
                .categories(categories.eachText())
                .build();
    }

    public List<String> readOutVideoTitles(Document doc) {
        Elements titleLinksWithDuplicates = doc.getElementsByAttributeValueStarting("href", "/video/");
        return titleLinksWithDuplicates.eachAttr("href")
                .stream()
                .distinct()
                .collect(Collectors.toList());
    }
}
