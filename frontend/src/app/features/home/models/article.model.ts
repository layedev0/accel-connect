export interface Article {
  id: string;
  /** The article's id. Despite the name, it addresses the article — used to fetch its cover. */
  documentId?: number;
  title: string;
  subtitle: string;
  category: string;
  categoryColor: string;
  coverImage: string;
  publishedDate?: string;
  authorName?: string;
  readTime: string;
  description?: string;
}
