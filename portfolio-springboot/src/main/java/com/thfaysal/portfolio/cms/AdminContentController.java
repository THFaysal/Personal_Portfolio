package com.thfaysal.portfolio.cms;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;

@Controller
@RequestMapping("/admin/content")
public class AdminContentController {
    private final PortfolioContentService content;
    public AdminContentController(PortfolioContentService content) { this.content=content; }

    private void privatePage(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
        response.setHeader("Referrer-Policy", "no-referrer");
    }

    @GetMapping
    public String index(Model model, HttpServletResponse response) {
        privatePage(response);
        model.addAttribute("items", content.allForAdmin());
        model.addAttribute("settings", content.settings());
        model.addAttribute("types", ContentType.values());
        return "admin/content/index";
    }

    @GetMapping("/new")
    public String add(@RequestParam(defaultValue="PROJECT") ContentType type, Model model,
                      HttpServletResponse response) {
        privatePage(response);
        PortfolioItemForm form = new PortfolioItemForm();
        form.setType(type);
        if (type != ContentType.PROJECT) form.setFeatured(false);
        model.addAttribute("form", form); model.addAttribute("editingId", "");
        model.addAttribute("heading", "Add " + type.getLabel());
        return "admin/content/edit";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model, HttpServletResponse response) {
        privatePage(response);
        PortfolioItem item = content.find(id).orElse(null);
        if (item == null) return "redirect:/admin/content?missing";
        model.addAttribute("form", PortfolioItemForm.from(item));
        model.addAttribute("editingId", item.getId());
        model.addAttribute("heading", "Edit " + item.getTitle());
        model.addAttribute("currentImageUrl", item.getImageId() == null ? item.getAssetPath()
                : "/admin/media/" + item.getImageId());
        return "admin/content/edit";
    }

    @PostMapping("/save")
    public String save(@RequestParam(value="id",required=false) String id,
                       @Valid @ModelAttribute("form") PortfolioItemForm form, BindingResult errors,
                       @RequestParam(value="image",required=false) MultipartFile image,
                       Model model, RedirectAttributes redirect, HttpServletResponse response) {
        privatePage(response);
        if (errors.hasErrors()) {
            model.addAttribute("heading", "Review content details");
            model.addAttribute("editingId", id == null ? "" : id);
            return "admin/content/edit";
        }
        try {
            content.save(id, form, image);
            redirect.addFlashAttribute("notice", "Saved. Published changes appear on your portfolio automatically.");
            return "redirect:/admin/content";
        } catch (IllegalArgumentException | IOException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("heading", "Review content details");
            model.addAttribute("editingId", id == null ? "" : id);
            return "admin/content/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirect) {
        content.delete(id);
        redirect.addFlashAttribute("notice", "Item deleted from the portfolio.");
        return "redirect:/admin/content";
    }

    @PostMapping("/photo")
    public String photo(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) throws IOException {
        try { content.uploadProfile(file); redirect.addFlashAttribute("notice", "Profile photo updated."); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/content";
    }

    @PostMapping("/photo/reset")
    public String photoReset(RedirectAttributes redirect) {
        content.resetProfile(); redirect.addFlashAttribute("notice", "Bundled profile photo restored.");
        return "redirect:/admin/content";
    }

    @PostMapping("/cv")
    public String cv(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) throws IOException {
        try { content.uploadCv(file); redirect.addFlashAttribute("notice", "CV replaced. The public Download CV links now serve the new PDF."); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/content";
    }

    @PostMapping("/cv/reset")
    public String cvReset(RedirectAttributes redirect) {
        content.resetCv(); redirect.addFlashAttribute("notice", "Bundled Corporate IT CV restored.");
        return "redirect:/admin/content";
    }
}
