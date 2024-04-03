package com.andreidodu.europealibrary.batch.indexer.config.step;

import com.andreidodu.europealibrary.batch.indexer.step.filehash.FileSystemItemHashProcessor;
import com.andreidodu.europealibrary.batch.indexer.step.filehash.FileSystemItemHashWriter;
import com.andreidodu.europealibrary.model.FileSystemItem;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.PostgresPagingQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.orm.hibernate5.HibernateTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class FileHashStepConfig {
    @Value("${com.andreidodu.europea-library.job.indexer.step-step-updater.batch-size}")
    private Integer batchSize;
    private final JobRepository jobRepository;
    private final TaskExecutor threadPoolTaskExecutor;
    private final JdbcPagingItemReader<FileSystemItem> hashStorerReader;
    private final FileSystemItemHashProcessor processor;
    private final FileSystemItemHashWriter writer;
    private final HibernateTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean("fileSystemItemHashStep")
    public Step fileSystemItemHashStep() {
        return new StepBuilder("fileSystemItemHashStep", jobRepository)
                .<FileSystemItem, FileSystemItem>chunk(batchSize, transactionManager)
                .allowStartIfComplete(true)
                .taskExecutor(threadPoolTaskExecutor)
                .reader(hashStorerReader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean("hashStorerReader")
    public JdbcPagingItemReader<FileSystemItem> hashStorerReader() {
        JdbcPagingItemReader<FileSystemItem> jdbcPagingItemReader = (new JdbcPagingItemReader<>());
        jdbcPagingItemReader.setDataSource(dataSource);
        jdbcPagingItemReader.setFetchSize(batchSize);
        jdbcPagingItemReader.setRowMapper(new BeanPropertyRowMapper<>(FileSystemItem.class));
        jdbcPagingItemReader.setQueryProvider(getPostgresHashQueryProvider());
        jdbcPagingItemReader.setSaveState(false);
        return jdbcPagingItemReader;
    }

    public PostgresPagingQueryProvider getPostgresHashQueryProvider() {
        PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();
        queryProvider.setSelectClause("SELECT *");
        queryProvider.setFromClause("FROM el_file_system_item");
        queryProvider.setWhereClause("WHERE record_status = 1");
        Map<String, Order> orderByKeys = new HashMap<>();
        orderByKeys.put("id", Order.ASCENDING);
        queryProvider.setSortKeys(orderByKeys);
        return queryProvider;
    }

}