package cn.wildfirechat.app;

import org.springframework.web.bind.annotation.RequestMapping;

@org.springframework.stereotype.Controller
public class WebController {

    @RequestMapping("/")
    public String homepage() {
        return "redirect:/index.html";
    }
}
