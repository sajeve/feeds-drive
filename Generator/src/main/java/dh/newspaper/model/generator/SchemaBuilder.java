package dh.newspaper.model.generator;

import de.greenrobot.daogenerator.*;

public class SchemaBuilder {
	public static void main(String[] args) throws Exception {
		System.out.println("Start generate..");

		Schema schema = new Schema(100, "dh.newspaper.model.generated");

		addPathToContent(schema);
		addArticle(schema);

		new DaoGenerator().generateAll(schema, "../Newspaper/src/main/java");

		System.out.println("Terminated OK");
	}

	private static void addPathToContent(Schema schema) {
		Entity entity = schema.addEntity("PathToContent");
		entity.addIdProperty();
		entity.addStringProperty("urlPattern").notNull().unique();
		entity.addStringProperty("xpath");
		entity.addStringProperty("language");
		entity.addBooleanProperty("enable");
		entity.addDateProperty("lastUpdate");
	}

	private static void addArticle(Schema schema) {
		Entity article = schema.addEntity("Article");
		article.addIdProperty();

		article.addStringProperty("articleUrl").notNull().unique();
		article.addStringProperty("parentUrl");
		article.addStringProperty("imageUrl");
		article.addStringProperty("title").notNull();
		article.addStringProperty("author");
		article.addStringProperty("excerpt").notNull();
		article.addStringProperty("content");
		article.addStringProperty("language");
		article.addDateProperty("published").notNull();
		article.addDateProperty("archived");
		article.addDateProperty("lastRead");
		article.addDateProperty("lastUpdated");

		Entity articleCategory = schema.addEntity("ArticleCategory");
		articleCategory.addIdProperty();
		articleCategory.addLongProperty("categoryId");
		Property articleId = articleCategory.addLongProperty("articleId").notNull().getProperty();
		ToMany toMany = article.addToMany(articleCategory, articleId);
	}
}
