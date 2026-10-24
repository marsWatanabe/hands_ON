package com.aojt.mgmt.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 部品在庫（テーブル {@code parts_stock_info}）。カテゴリの種別は {@link Category#TYPE_PART}。
 */
@Entity
@Table(name = "parts_stock_info")
public class PartsStock extends StockItem {
}
