package com.thatguyalex.rk2023.presentation

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping


@Controller
class Html5PathsController {
    @RequestMapping("{path:[^.]*}")
    fun redirect(): String {
        return "forward:/index.html"
    }
}