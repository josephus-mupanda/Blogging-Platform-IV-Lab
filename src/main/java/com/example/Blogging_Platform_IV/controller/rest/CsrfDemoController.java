package com.example.Blogging_Platform_IV.controller.rest;

import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

// Demo of the CSRF token. This is the only chain with CSRF protection on (see SecurityConfig).
// GET /csrf-demo/form: Spring creates a session and a token, and the form contains it (hidden field _csrf).
// POST /csrf-demo/submit: Spring compares the _csrf field with the token of the session. No match: 403.
@RestController
@RequestMapping("/csrf-demo")
public class CsrfDemoController {

    @GetMapping(value = "/form", produces = MediaType.TEXT_HTML_VALUE)
    public String form(CsrfToken csrf) {
        return """
                <!doctype html>
                <html><head><meta charset="utf-8"><title>CSRF demo</title></head>
                <body style="font-family: sans-serif; max-width: 40em; margin: 2em auto">
                  <h2>CSRF demo</h2>
                  <h3>1. Form with the CSRF token</h3>
                  <form method="post" action="/csrf-demo/submit">
                    <input type="hidden" name="%s" value="%s">
                    <input name="message" value="hello" required>
                    <button>Send</button>
                  </form>
                  <p>Accepted: the hidden field matches the token stored in the session.</p>
                  <h3>2. Same form without the token (what a forged request looks like)</h3>
                  <form method="post" action="/csrf-demo/submit">
                    <input name="message" value="forged" required>
                    <button>Send</button>
                  </form>
                  <p>Refused with 403: there is no token.</p>
                </body></html>
                """.formatted(HtmlUtils.htmlEscape(csrf.getParameterName()), HtmlUtils.htmlEscape(csrf.getToken()));
    }

    @PostMapping(value = "/submit", produces = MediaType.TEXT_HTML_VALUE)
    public String submit(@RequestParam String message) {
        return "<p>Accepted. The CSRF token was valid. Message: " + HtmlUtils.htmlEscape(message)
                + "</p><p><a href=\"/csrf-demo/form\">Back</a></p>";
    }
}
