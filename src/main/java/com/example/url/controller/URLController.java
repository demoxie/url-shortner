package com.example.url.controller;

import com.example.url.model.Url;
;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.charset.Charset;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@RequestMapping("/api/v1")
@RestController
@CrossOrigin(origins = "http://localhost:9008/api/v1",maxAge = 3600)
public class URLController {
    @Autowired
    StringRedisTemplate redisTemplate;

    @SneakyThrows
    @RequestMapping(value = "/url/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getUrl(@PathVariable String id, HttpServletResponse response) throws IOException {

        // get from redis

        String url = redisTemplate.opsForValue().get(id);
        Gson gson = new Gson();
        Url u = gson.fromJson(url, Url.class);

        if (Objects.isNull(u)) {
            assert false;
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No such  URL exist: " + u.getUrl());
        }
        u.setNoOfTimesVisited(u.getNoOfTimesVisited()+1);
        redisTemplate.opsForValue().set(u.getId(), new Gson().toJson(u));
        response.sendRedirect(u.getUrl());
        return null;
    }
    @RequestMapping(value = "/urlhistory/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getUrlHistory(@PathVariable String id) {

        // get from redis

        String url = redisTemplate.opsForValue().get(id);
        Gson gson = new Gson();
        Url u = gson.fromJson(url, Url.class);

        if (Objects.isNull(u)) {
            assert false;
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No such  URL exist: " + u.getUrl());
        }
        redisTemplate.opsForValue().set(u.getId(), new Gson().toJson(u));

        return ResponseEntity.ok(u);
    }

    @PostMapping("/shortenurl")
    @ResponseBody
    public ResponseEntity create(@RequestBody @NotNull Url url) {

        UrlValidator validator = new UrlValidator(
                new String[]{"http", "https"}
        );


        if (!validator.isValid(url.getUrl())) {

            return ResponseEntity.badRequest().body("Invalid URL: " + url.getUrl());
        }

        // generating murmur3 based hash key as short URL. [3]
        String id = Hashing.murmur3_32().hashString(url.getUrl(), Charset.defaultCharset()).toString();
        //Get current date time
        LocalDateTime now = LocalDateTime.now();


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String formatDateTime = now.format(formatter);
        url.setId(id);
        url.setCreatedAt(formatDateTime);
        String str =  new Gson().toJson(url);
        redisTemplate.opsForValue().set(url.getId(),str);
        //,600000, TimeUnit.SECONDS

        return ResponseEntity.ok(url);
    }
}

