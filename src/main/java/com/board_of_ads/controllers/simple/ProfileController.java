package com.board_of_ads.controllers.simple;

import com.board_of_ads.models.User;
import com.board_of_ads.models.dto.order.Order;
import com.board_of_ads.service.interfaces.OrderService;
import com.board_of_ads.service.interfaces.ReviewService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Set;

@Controller
@AllArgsConstructor
@Slf4j
public class ProfileController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final OrderService orderService;
    private final ReviewService reviewService;



    @GetMapping("/orders/sales")
    public String getSales(@AuthenticationPrincipal User user, Model model){
        Set<Order> sales = orderService.getOrdersByOwner(user);
        sales.forEach(x->{
            x.setChecked(true);
            orderService.save(x);
        });

        model.addAttribute(user);
        model.addAttribute("page", "sales");
        model.addAttribute("content", sales);
        model.addAttribute("sales", sales.size());
        model.addAttribute("purchases", orderService.countOrdersByUser(user));
        model.addAttribute("unchecked_orders", 0);
        return "profile_pages/orders";
    }

    @GetMapping("/orders/purchases")
    public String getPurchases(@AuthenticationPrincipal User user, Model model){
        Set<Order> purchases = orderService.getOrdersByUser(user);

        model.addAttribute(user);
        model.addAttribute("page", "purchases");
        model.addAttribute("content", purchases);
        model.addAttribute("sales", orderService.countOrdersByOwner(user));
        model.addAttribute("purchases", purchases.size());
        model.addAttribute("unchecked_orders", orderService.countUncheckedByUser(user));
        return "profile_pages/orders";
    }

    @GetMapping("/profile/contacts")
    public String getReviews(@AuthenticationPrincipal User user, Model model){
        model.addAttribute(user);
        model.addAttribute("unchecked_orders", orderService.countUncheckedByUser(user));
        model.addAttribute("page", "contacts");
//        TODO: Сделать вывод всех заказов, по которым была переписка за последние 2 недели
        model.addAttribute("content", new ArrayList<>());
        return "profile_pages/contacts";
    }

    @GetMapping("/profile/reviews")
    public String getLeftReviews(@AuthenticationPrincipal User user, Model model){
        model.addAttribute(user);
        model.addAttribute("unchecked_orders", orderService.countUncheckedByUser(user));
        model.addAttribute("page", "reviews");
        model.addAttribute("content", reviewService.getReviewsByUser(user));
        return "profile_pages/reviews";
    }
}