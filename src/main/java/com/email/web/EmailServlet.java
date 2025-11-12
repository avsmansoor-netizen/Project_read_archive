package com.Email.web;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import jakarta.mail.Flags;
import jakarta.mail.Flags.Flag;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.mail.*;
import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Folder;
import jakarta.mail.search.FlagTerm;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@WebServlet(name = "EmailServlet", urlPatterns = {"/process"})
public class EmailServlet extends HttpServlet {

    private static final String CREDENTIALS_PATH = "/credentials.csv";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<html><head><meta charset='utf-8'><title>Email WebApp - Email Processor</title>");
        out.println("<style>body{font-family:Arial,Helvetica,sans-serif;margin:20px} table{border-collapse:collapse;width:100%} td,th{border:1px solid #ccc;padding:8px}</style>");
        out.println("</head><body>");
        out.println("<h2>Email processor — reading INBOX and archiving</h2>");
        out.println("<p>Note: IMAP must be enabled and Gmail app-password used (or OAuth).</p>");

        List<AccountResult> results = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream(CREDENTIALS_PATH);
        if (is == null) {
            out.println("<p style='color:red'>No credentials.csv found in classpath (place it under src/main/resources/credentials.csv)</p>");
            out.println("</body></html>");
            return;
        }

        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreEmptyLines(true).parse(reader);

        for (CSVRecord record : records) {
            String email = record.get("email").trim();
            String password = record.get("password").trim();
            AccountResult ar = processAccount(email, password);
            results.add(ar);
        }

        out.println("<table>");
        out.println("<tr><th>Account</th><th>Processed</th><th>Archived</th><th>Errors</th></tr>");
        for (AccountResult r : results) {
            out.printf("<tr><td>%s</td><td>%d</td><td>%d</td><td><pre>%s</pre></td></tr>%n",
                    escapeHtml(r.email), r.processedCount, r.archivedCount, escapeHtml(r.error == null ? "" : r.error));
        }
        out.println("</table>");
        out.println("<p><a href='/'>Back</a></p>");
        out.println("</body></html>");
    }

    private AccountResult processAccount(String email, String password) {
        AccountResult result = new AccountResult(email);
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.ssl.trust", "*");

        Session session = Session.getInstance(props);
        try (Store store = session.getStore("imaps")) {
            store.connect("imap.gmail.com", 993, email, password);
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            Message[] messages = inbox.getMessages();
            for (Message msg : messages) {
                if (!msg.isSet(Flag.SEEN)) {
                    result.processedCount++;
                    msg.setFlag(Flag.SEEN, true);
                    Folder archive = store.getFolder("[Gmail]/All Mail");
                    if (!archive.exists()) archive = store.getFolder("All Mail");
                    if (!archive.exists()) {
                        archive = store.getFolder("Archive");
                        if (!archive.exists()) archive.create(Folder.HOLDS_MESSAGES);
                    }
                    if (!archive.isOpen()) archive.open(Folder.READ_WRITE);
                    inbox.copyMessages(new Message[]{msg}, archive);
                    msg.setFlag(Flag.DELETED, true);
                    result.archivedCount++;
                }
            }
            inbox.close(true);
            store.close();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.error = sw.toString();
        }
        return result;
    }

    private static class AccountResult {
        String email;
        int processedCount = 0;
        int archivedCount = 0;
        String error = null;
        AccountResult(String email) { this.email = email; }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
