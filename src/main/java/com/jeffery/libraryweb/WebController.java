package com.jeffery.libraryweb;

import jakarta.servlet.http.HttpSession;
import model.Book;
import model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.LibraryService;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class WebController {
    private final LibraryService libraryService;

    public WebController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/")
    public String home(@RequestParam(value = "suspended", required = false) String suspended,
                       Model model) {
        if ("1".equals(suspended)) {
            model.addAttribute("message", "Your session ended because this account is suspended.");
        }
        return "index";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String email,
                           @RequestParam String role,
                           Model model) {
        String r = role.toUpperCase();
        if (!"STUDENT".equals(r) && !"ADMIN".equals(r)) {
            model.addAttribute("message", "Employee accounts can only be assigned by a library administrator.");
            return "register";
        }
        boolean created = libraryService.registerUser(
                new User(name, username, password, r, email));
        model.addAttribute("message", created ? "User registered successfully." : "Username already exists.");
        return "register";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        User user = libraryService.login(username, password);
        if (user == null) {
            if (libraryService.isSuspendedAccount(username, password)) {
                model.addAttribute("message", "This account has been suspended.");
            } else {
                model.addAttribute("message", "Invalid credentials.");
            }
            return "index";
        }
        session.setAttribute("user", user);
        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin";
        }
        if ("EMPLOYEE".equals(user.getRole())) {
            return "redirect:/employee";
        }
        return "redirect:/student";
    }

    @GetMapping("/admin")
    public String adminPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }
        model.addAttribute("books", libraryService.getBooks());
        model.addAttribute("users", libraryService.getUsers());
        model.addAttribute("actorId", user.getId());
        model.addAttribute("actorRole", user.getRole());
        return "admin";
    }

    @PostMapping("/admin/add-book")
    public String addBook(@RequestParam String title,
                          @RequestParam String author,
                          @RequestParam String isbn,
                          @RequestParam int quantity,
                          @RequestParam(required = false) String summary,
                          HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }
        String s = summary != null ? summary.trim() : "";
        libraryService.addBook(new Book(title, author, isbn, quantity, s));
        return "redirect:/admin";
    }

    @PostMapping("/admin/user-role")
    public String updateUserRole(@RequestParam String userId,
                                 @RequestParam String newRole,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            return "redirect:/";
        }
        String msg = libraryService.updateUserRoleByAdmin(userId, newRole);
        redirectAttributes.addFlashAttribute("adminMessage", msg);
        return "redirect:/admin";
    }

    @PostMapping("/admin/remove-user")
    public String removeUser(@RequestParam String userId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            return "redirect:/";
        }
        String msg = libraryService.deleteUserByAdmin(admin.getId(), userId);
        redirectAttributes.addFlashAttribute("adminMessage", msg);
        return "redirect:/admin";
    }

    @PostMapping("/staff/set-ban")
    public String setUserBan(@RequestParam String userId,
                             @RequestParam boolean blocked,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User actor = (User) session.getAttribute("user");
        if (actor == null || (!"ADMIN".equals(actor.getRole()) && !"EMPLOYEE".equals(actor.getRole()))) {
            return "redirect:/";
        }
        String msg = libraryService.setUserBan(actor.getRole(), actor.getId(), userId, blocked);
        redirectAttributes.addFlashAttribute("staffMessage", msg);
        if ("ADMIN".equals(actor.getRole())) {
            return "redirect:/admin";
        }
        return "redirect:/employee";
    }

    @GetMapping("/employee")
    public String employeePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"EMPLOYEE".equals(user.getRole())) {
            return "redirect:/";
        }
        model.addAttribute("books", libraryService.getBooks());
        model.addAttribute("users", libraryService.getUsers());
        model.addAttribute("issues", libraryService.getIssues());
        Map<String, User> userMap = libraryService.getUsers().stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        model.addAttribute("userMap", userMap);
        model.addAttribute("actorId", user.getId());
        model.addAttribute("actorRole", user.getRole());
        return "employee";
    }

    @GetMapping("/student")
    public String studentPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"STUDENT".equals(user.getRole())) {
            return "redirect:/";
        }
        model.addAttribute("books", libraryService.getBooks());
        model.addAttribute("issues", libraryService.getIssues());
        return "student";
    }

    @PostMapping("/student/issue")
    public String issueBook(@RequestParam String bookId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"STUDENT".equals(user.getRole())) {
            return "redirect:/";
        }
        String message = libraryService.issueBook(bookId, user.getId());
        model.addAttribute("message", message);
        model.addAttribute("books", libraryService.getBooks());
        model.addAttribute("issues", libraryService.getIssues());
        return "student";
    }

    @PostMapping("/student/return")
    public String returnBook(@RequestParam String bookId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"STUDENT".equals(user.getRole())) {
            return "redirect:/";
        }
        String message = libraryService.returnBook(bookId, user.getId());
        model.addAttribute("message", message);
        model.addAttribute("books", libraryService.getBooks());
        model.addAttribute("issues", libraryService.getIssues());
        return "student";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
