package com.example.Blogging_Platform_IV.controller.rest;

import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * A small form-based endpoint that shows the CSRF token mechanism. It is protected by its own
 * security filter chain (see SecurityConfig), the only one where CSRF protection is switched on,
 * because a form served to a browser works with a session cookie.
 *
 * How it works:
 *   1. GET /csrf-demo/form  Spring creates a session and a random CSRF token stored in that session.
 *      The page contains the token in a hidden field called _csrf.
 *   2. POST /csrf-demo/submit  Spring compares the _csrf field with the token in the session.
 *      Same value: the request runs. Missing or different value: 403 and the method is not called.
 *
 * Another website cannot copy the token: it cannot read this page (same-origin policy), so a form
 * it submits in the user's browser carries the session cookie but not the token, and is refused.
 */
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
