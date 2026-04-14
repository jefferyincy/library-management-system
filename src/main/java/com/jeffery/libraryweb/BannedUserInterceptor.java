package com.jeffery.libraryweb;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import service.LibraryService;

import java.util.Optional;

@Component
public class BannedUserInterceptor implements HandlerInterceptor {
    private final LibraryService libraryService;

    public BannedUserInterceptor(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return true;
        }
        Optional<User> fresh = libraryService.findUserById(sessionUser.getId());
        if (fresh.isEmpty() || fresh.get().isBanned()) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/?suspended=1");
            return false;
        }
        session.setAttribute("user", fresh.get());
        return true;
    }
}
