package com.cybershield.controller;

import com.cybershield.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Full results page for the global search (used when JavaScript is disabled or Enter is pressed). */
@Controller
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(name = "q", defaultValue = "") String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("results", searchService.search(q));
        return "search/results";
    }
}
