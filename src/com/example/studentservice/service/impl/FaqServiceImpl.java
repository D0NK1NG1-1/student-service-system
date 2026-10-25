package com.example.studentservice.service.impl;

import com.example.studentservice.dao.FaqDao;
import com.example.studentservice.dao.impl.FaqDaoImpl;
import com.example.studentservice.entity.Faq;
import com.example.studentservice.service.FaqService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FAQ Service — AI 问答链路的第一步
 * <p>
 * matchQuestion 核心算法:
 *   1) 先试完整句子 LIKE (最快精确路径)
 *   2) 未命中 → 2/3-gram 滑窗分词, 每个 token 单独 LIKE, 合并去重
 *      e.g. "图书馆时间" → ["图书","图书馆","书馆","馆时","时间"]
 *           LIKE "%图书馆%" 就能命中 keywords="图书馆,开放时间,几点开门"
 */
public class FaqServiceImpl implements FaqService {

    private final FaqDao faqDao = new FaqDaoImpl();

    @Override public Faq create(Faq faq) { long id = faqDao.insert(faq); faq.setId(id); return faq; }
    @Override public boolean update(Faq faq) { return faqDao.update(faq) > 0; }
    @Override public boolean delete(Long id) { return faqDao.deleteById(id) > 0; }
    @Override public Faq getById(Long id) { return faqDao.selectById(id); }
    @Override public List<Faq> listAll() { return faqDao.selectAll(); }
    @Override public List<Faq> listPage(int page, int size) { return faqDao.selectPage(page, size); }
    @Override public long totalCount() { return faqDao.count(); }
    @Override public List<Faq> searchByKeyword(String keyword) { return faqDao.selectByKeyword(keyword); }
    @Override public List<Faq> listByCategory(String category) { return faqDao.selectByCategory(category); }

    @Override
    public Faq matchQuestion(String question) {
        if (question == null || question.trim().isEmpty()) return null;
        String q = question.trim();

        // 1) 完整句子 LIKE (精确/快速路径)
        List<Faq> candidates = faqDao.selectByKeyword(q);
        if (candidates != null && !candidates.isEmpty()) {
            faqDao.incrementHitCount(candidates.get(0).getId());
            return candidates.get(0);
        }

        // 2) 2/3-gram 滑窗分词, 每个 token 单独 LIKE, 合并去重
        // 这样 "图书馆时间" 拆成 "图书/图书馆/书馆/馆时/时间" 就能命中 keywords="图书馆,..."
        Set<Long> seen = new HashSet<>();
        List<Faq> merged = new ArrayList<>();
        int len = q.length();
        for (int i = 0; i < len - 1; i++) {
            for (int gram = 2; gram <= 3 && i + gram <= len; gram++) {
                String token = q.substring(i, i + gram);
                List<Faq> partial = faqDao.selectByKeyword(token);
                if (partial != null) {
                    for (Faq f : partial) {
                        if (seen.add(f.getId())) merged.add(f);
                    }
                }
            }
        }

        if (!merged.isEmpty()) {
            faqDao.incrementHitCount(merged.get(0).getId());
            return merged.get(0);
        }
        return null;  // 真没命中, 上层调 LLM
    }
}
