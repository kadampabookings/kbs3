package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link SourceClient} answering from recorded public responses ({@code recorded/} in the test resources: products
 * and versions chosen because no deployment of this module is meant to run them), with failures that can be injected
 * per URL fragment. Records every URL asked, for the checks to assert on.
 */
class RecordedSourceClient implements SourceClient {

    final List<String> asked = new ArrayList<>();
    /** URL fragment → the failure to answer with (a code), or an HTTP status as "status:429". */
    final Map<String, String> failures = new LinkedHashMap<>();
    /** URL fragment → how many times to fail before answering normally (for the retry). */
    final Map<String, Integer> failTimes = new LinkedHashMap<>();

    static String resource(String name) {
        try (InputStream in = RecordedSourceClient.class.getResourceAsStream(name)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Future<Reply> get(String url, int maxBytes, long timeoutMillis) {
        return answer(url, null, maxBytes);
    }

    @Override
    public Future<Reply> post(String url, String jsonBody, int maxBytes, long timeoutMillis) {
        return answer(url, jsonBody, maxBytes);
    }

    private Future<Reply> answer(String url, String body, int maxBytes) {
        asked.add(url);
        if (!SourceClient.isAllowed(url))
            return Future.failedFuture(new SourceFailure(SourceFailure.INVALID_RESPONSE));
        for (Map.Entry<String, String> f : failures.entrySet())
            if (url.contains(f.getKey())) {
                Integer times = failTimes.get(f.getKey());
                if (times != null) {
                    if (times <= 0)
                        continue;
                    failTimes.put(f.getKey(), times - 1);
                }
                if (f.getValue().startsWith("status:"))
                    return Future.succeededFuture(new Reply(Integer.parseInt(f.getValue().substring(7)), "{}"));
                return Future.failedFuture(new SourceFailure(f.getValue()));
            }
        String text = text(url, body);
        if (text == null)
            return Future.succeededFuture(new Reply(404, "{\"message\":\"not recorded\"}"));
        if (text.getBytes(StandardCharsets.UTF_8).length > maxBytes)
            return Future.failedFuture(new SourceFailure(SourceFailure.TOO_LARGE));
        return Future.succeededFuture(new Reply(200, text));
    }

    private String text(String url, String body) {
        if (url.startsWith(OsvSource.BATCH_URL))
            return osvBatch(body);
        if (url.startsWith(OsvSource.VULN_URL))
            return resource("recorded/osv-vuln-" + url.substring(OsvSource.VULN_URL.length()) + ".json");
        if (url.startsWith(NvdSource.URL)) {
            if (url.contains("virtualMatchString=cpe%3A2.3%3Aa%3Aredis%3Aredis%3A7.0.0"))
                return resource("recorded/nvd-version.json");
            if (url.contains("keywordSearch="))
                return resource("recorded/nvd-keyword.json");
            return "{\"resultsPerPage\":0,\"startIndex\":0,\"totalResults\":0,\"vulnerabilities\":[]}";
        }
        if (url.equals(EndOfLifeSource.URL + "redis/"))
            return resource("recorded/endoflife-product.json");
        if (url.startsWith(DockerHubSource.URL))
            return resource("recorded/dockerhub-tag.json");
        if (url.equals(KevSource.URL))
            return resource("recorded/kev.json");
        return null;
    }

    /** The batch answer for the queries asked: the recorded flaws of the two recorded packages, none for the rest. */
    private static String osvBatch(String body) {
        List<Object> results = new ArrayList<>();
        for (Object item : JsonTree.list(JsonTree.parseObject(body), "queries")) {
            Map<String, Object> q = JsonTree.asObject(item);
            String name = JsonTree.string(JsonTree.object(q, "package"), "name"), version = JsonTree.string(q, "version");
            List<Object> vulns = new ArrayList<>();
            if ("minimist".equals(name) && "1.2.5".equals(version))
                vulns.add(Map.of("id", "GHSA-xvch-5gv4-984h"));
            if ("org.apache.struts:struts2-core".equals(name) && "2.5.10".equals(version)) {
                vulns.add(Map.of("id", "GHSA-j77q-2qqg-6989"));
                vulns.add(Map.of("id", "GHSA-xg75-68x3-7p3q"));
            }
            results.add(vulns.isEmpty() ? Map.of() : Map.of("vulns", vulns));
        }
        return JsonTree.write(Map.of("results", results));
    }
}
