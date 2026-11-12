package com.cybershield.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Central error handling for the server-rendered pages.
 * Users only ever see friendly messages - never SQL, stack traces or internal exception text.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    public ModelAndView notFound(Exception e) {
        return errorPage("error/404", HttpStatus.NOT_FOUND,
                e instanceof ResourceNotFoundException ? e.getMessage() : "The page you requested does not exist.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView accessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.info("Access denied for {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return errorPage("error/403", HttpStatus.FORBIDDEN, e.getMessage());
    }

    /** Business rule violations: go back to the previous page with an error message. */
    @ExceptionHandler(BusinessRuleException.class)
    public ModelAndView businessRule(BusinessRuleException e, HttpServletRequest request) {
        return redirectBack(request, e.getMessage());
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class})
    public ModelAndView uploadTooLarge(HttpServletRequest request) {
        return redirectBack(request, "The file is larger than the 10 MB limit.");
    }

    @ExceptionHandler(MultipartException.class)
    public ModelAndView badUpload(HttpServletRequest request) {
        return redirectBack(request, "The upload could not be processed. Please choose a valid file (max 10 MB).");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView dataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Database constraint violation on {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
        return redirectBack(request, "The change was rejected by the database because it conflicts with existing data "
                + "(for example a duplicate value or a record that is still referenced).");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ModelAndView badRequest() {
        return errorPage("error/error", HttpStatus.BAD_REQUEST, "The request contained invalid or missing values.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView methodNotAllowed() {
        return errorPage("error/error", HttpStatus.METHOD_NOT_ALLOWED, "This action is not available here.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return errorPage("error/500", HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side. The problem has been logged.");
    }

    private static ModelAndView errorPage(String view, HttpStatus status, String message) {
        ModelAndView mv = new ModelAndView(view);
        mv.setStatus(status);
        mv.addObject("status", status.value());
        mv.addObject("error", status.getReasonPhrase());
        mv.addObject("message", message);
        return mv;
    }

    /** Redirects to the Referer (same application only) and shows the message as a flash error. */
    static ModelAndView redirectBack(HttpServletRequest request, String message) {
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("error", message);
        return new ModelAndView("redirect:" + safeReferer(request));
    }

    static String safeReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                boolean sameHost = uri.getHost() == null || uri.getHost().equalsIgnoreCase(request.getServerName());
                String path = uri.getRawPath();
                if (sameHost && path != null && path.startsWith("/") && !path.startsWith("//")) {
                    return path + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
                }
            } catch (IllegalArgumentException ignored) {
                // fall through to the dashboard
            }
        }
        return "/dashboard";
    }
}
