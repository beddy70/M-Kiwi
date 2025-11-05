package org.somanybits.minitel;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.somanybits.minitel.components.MFrame;
import org.somanybits.minitel.components.MText;
import org.somanybits.minitel.components.MTitle;

public class ReadNews {

    private int count = 0;
    private Elements articles;

    public ReadNews(String url) throws Exception {
        boolean save = true;

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Java JSoup)")
                .timeout(15_000)
                .get();

        articles = doc.select("article");

        this.count = articles.size();
    }

    public MFrame readPage() {

        MFrame frame = new MFrame(38, 4);

        int i = 0;

        for (Element art : articles) {
            
            // TITLE
            Element h2 = art.select("h2.entry-title").first();
            Element ancrage = h2.select("a").first();
            MTitle title = new MTitle(ancrage.nodeValue()+"\r"+String.valueOf(new char[]{ 0x1B, 0x5B, 0x88}), 0);
            frame.addMComponent(title);

            //Date
            //Element time = art.select("time").get(0);
            //System.out.println("Date=" + time.nodeValue());

            //content
            Elements paraph = art.select("p");

            for (Element p : paraph) {
                //System.out.println("\t >" + p.nodeValue());
                MText text = new MText(p.nodeValue()+"\r"+String.valueOf(new char[]{ 0x1B, 0x5B, 0x88}));
                frame.addMComponent(text);
            }

            
            i++;

//            String outer = art.outerHtml(); // tout le bloc <article>…</article>
//            System.out.println("=== ARTICLE #" + i + " ===");
//            System.out.println(outer);
//            System.out.println();

//            if (save) {
//                // Écrit chaque article dans un fichier séparé (optionnel)
//                Path out = Path.of(String.format("article-%02d.html", i));
//                Files.writeString(out, outer, StandardCharsets.UTF_8);
//            }
        }
        return frame;
    }

}
