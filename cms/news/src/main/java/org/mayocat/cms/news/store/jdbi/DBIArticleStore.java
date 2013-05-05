package org.mayocat.cms.news.store.jdbi;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.mayocat.cms.news.model.Article;
import org.mayocat.cms.news.store.ArticleStore;
import org.mayocat.model.Addon;
import org.mayocat.store.EntityAlreadyExistsException;
import org.mayocat.store.EntityDoesNotExistException;
import org.mayocat.store.InvalidEntityException;
import org.mayocat.store.StoreException;
import org.mayocat.store.rdbms.dbi.DBIEntityStore;
import mayoapp.dao.ArticleDAO;
import org.mayocat.addons.store.dbi.AddonsHelper;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

/**
 * @version $Id$
 */
@Component(hints = { "jdbi", "default" })
public class DBIArticleStore extends DBIEntityStore implements ArticleStore, Initializable
{
    private ArticleDAO dao;

    private static final String ARTICLE_TABLE_NAME = "article";

    @Override
    public Article create(@Valid Article article) throws EntityAlreadyExistsException, InvalidEntityException
    {
        if (this.dao.findBySlug(ARTICLE_TABLE_NAME, article.getSlug(), getTenant()) != null) {
            throw new EntityAlreadyExistsException();
        }

        this.dao.begin();

        UUID entityId = UUID.randomUUID();
        article.setId(entityId);

        this.dao.createEntity(article, ARTICLE_TABLE_NAME, getTenant());
        this.dao.createArticle(article);
        this.dao.insertTranslations(entityId, article.getTranslations());
        this.dao.createOrUpdateAddons(article);

        this.dao.commit();
        return article;
    }

    @Override
    public void update(@Valid Article article) throws EntityDoesNotExistException, InvalidEntityException
    {
        this.dao.begin();

        Article originalArticle = this.findBySlug(article.getSlug());
        if (originalArticle == null) {
            this.dao.commit();
            throw new EntityDoesNotExistException();
        }
        article.setId(originalArticle.getId());
        Integer updatedRows = this.dao.updateArticle(article);
        this.dao.createOrUpdateAddons(article);

        this.dao.commit();

        if (updatedRows <= 0) {
            throw new StoreException("No rows was updated when updating article");
        }
    }

    @Override
    public void delete(@Valid Article entity) throws EntityDoesNotExistException
    {
        Integer updatedRows = 0;
        this.dao.begin();
        updatedRows += this.dao.deleteAddons(entity);
        updatedRows += this.dao.deleteEntityEntityById(ARTICLE_TABLE_NAME, entity.getId());
        updatedRows += this.dao.detachChildren(entity.getId());
        updatedRows += this.dao.deleteEntityAndChildrenById(entity.getId());
        this.dao.commit();

        if (updatedRows <= 0) {
            throw new EntityDoesNotExistException("No rows was updated when trying to delete article");
        }
    }

    @Override
    public Integer countAll()
    {
        return this.dao.countAll(ARTICLE_TABLE_NAME, getTenant());
    }

    @Override
    public List<Article> findAll(Integer number, Integer offset)
    {
        return AddonsHelper.withAddons(this.dao.findAll(ARTICLE_TABLE_NAME, getTenant(), number, offset), this.dao);
    }

    @Override
    public List<Article> findByIds(List<UUID> ids)
    {
        return AddonsHelper.withAddons(this.dao.findByIds(ARTICLE_TABLE_NAME, ids), this.dao);
    }

    @Override
    public Article findBySlug(String slug)
    {
        Article article = this.dao.findBySlugWithTranslations(ARTICLE_TABLE_NAME, slug, getTenant());
        if (article != null) {
            List<Addon> addons = this.dao.findAddons(article);
            article.setAddons(addons);
        }
        return article;
    }

    @Override
    public List<Article> findAllPublished(Integer offset, Integer number)
    {
        return AddonsHelper.withAddons(this.dao.findAllPublished(getTenant(), number, offset), this.dao);
    }

    @Override
    public Article findById(UUID id)
    {
        Article article = this.dao.findById(ARTICLE_TABLE_NAME, id);
        List<Addon> addons = this.dao.findAddons(article);
        article.setAddons(addons);
        return article;
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.dao = this.getDbi().onDemand(ArticleDAO.class);
        super.initialize();
    }
}