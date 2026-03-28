package com.cognizant.medlab.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("requestURI")
    public String requestURI(final HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        return request.getRequestURI();
    }
}
