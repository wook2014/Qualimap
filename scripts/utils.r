if(!require("Repitools")) { source("http://bioconductor.org/biocLite.R"); biocLite("Repitools") }
suppressPackageStartupMessages(library(Repitools))
if(!require("GenomicFeatures")) { source("http://bioconductor.org/biocLite.R"); biocLite("GenomicFeatures") }
suppressPackageStartupMessages(library(GenomicFeatures))
library(GenomicFeatures)

buildAnnotation <- function(file.anno=NULL,geneSelection=NULL,organism="HUMAN"){
#TODO: Make it work for different organisms
#TODO: Allow to pass a file with the annotation previously dowloaded
#TODO: Think how to construct the annotation for other regions like CpG islands

  if(!is.null(file.anno)){
    annot <- read.delim(file.anno)
    
  }else{
  
    if(organism=="HUMAN"){
      if(is.null(geneSelection)){
        txdb <- makeTranscriptDbFromBiomart(biomart="ensembl",dataset="hsapiens_gene_ensembl")
      }else{
        tr.ids <- read.delim(geneSelection[1,"file"],header=F)[as.numeric(geneSelection[1,"column"])][[1]] # We select the column of interest and get the values as a factor ([[1]])
        print(paste("Building Annotation for the", length(tr.ids), "selected transcripts. Organism =",organism))
        txdb <- makeTranscriptDbFromBiomart(biomart="ensembl",dataset="hsapiens_gene_ensembl",transcript_ids=as.vector(tr.ids))
      }
    
    }

    annot <- txdb2GRanges(txdb)
  }
  annot
}

txdb2GRanges <- function(txdb){
  tmp=unfactor(as.list(txdb))
  # TODO: Potential problem with the chromosome names! Right now it is forcing to the format chr1 but maybe that the bam file has other staff
  annot=GRanges(seqnames=paste("chr",tmp$transcripts$tx_chrom,sep=""),ranges=IRanges(start=tmp$transcripts$tx_start,end=tmp$transcripts$tx_end),strand=tmp$transcripts$tx_strand,tx_name=tmp$transcripts$tx_name,tx_id=tmp$transcripts$tx_id,gene_id=tmp$genes$gene_id[match(tmp$transcripts$tx_id,tmp$genes$tx_id)])
  g2tx=values(annot)
                                        #Now just keep the longest transcript, that's what we want
  tmp=width(annot)
  names(tmp)=values(annot)$tx_name
  tmp=split(tmp,values(annot)$gene_id)
  tmp=lapply(tmp,function(u){u[which.max(u)]})
  tmp=sapply(tmp,names)
  annot=annot[match(tmp,values(annot)$tx_name)]
  
  annot
}

getBamFilesFromDFLine <- function(sample){
	#print("In getBamFilesFromDFLine")
	#print(sample)
	bam.files <- c()
	for(i in c("medips","input")){
		#print(paste(i,sample[i]))
		bam.files <- if(!is.na(sample[i])) c(bam.files, sample[i]) else bam.files
	}
	bam.files
}

getDataFromMatrix <- function(sample, names, with.NA=F){
	#print("In getDataFromMatrix")
        ret <- c()   
        for(i in 1:nrow(sample)){
		for(n in names){
	                ret <- if(with.NA | !is.na(sample[i,n])) c(ret,as.character(sample[i,n])) else ret
        	}
	}
        ret

}

getBamFilesFromMatrix <- function(sample){
	#OBSOLETE: Use getDataFromMatrix(sample1,c("medips","input"))
	ret -> getDataFromMatrix(sample1,c("medips","input"))
	ret
}

#TODO: Allow to pass the design
#TODO: Parse from the xml the paramenters up,down,freq,s.width
#TODO: Use the replicates and compose them
buildFeatureScores <- function(grl.stored=NULL, sample1=NULL, sample2=NULL, design=NULL, annot, up=2000,down=500,freq=100,s.width=300,dist="base"){
# Attention!!! If grl.stored is used, the variable for the GRangeList has to be called grl
  if(!is.null(grl.stored)){
    print(paste("Loading",grl.stored))
    load(grl.stored)
	sample.names <- names(grl) 
  }else{
	medips.files1 <- getDataFromMatrix(sample1,c("medips"))
	sample.types <- c(getDataFromMatrix(sample1,c("sample.name")))
	input.files1 <- getDataFromMatrix(sample1,c("input"))
	sample.types <- c(sample.types, rep(paste(getDataFromMatrix(sample1,c("sample.name"))[1],"input",sep="-"),length(input.files1)))
	medips.files2 <- getDataFromMatrix(sample2,c("medips"))
	sample.types <- c(sample.types, getDataFromMatrix(sample2,c("sample.name")))
	input.files2 <- getDataFromMatrix(sample2,c("input"))
	sample.types <- c(sample.types, rep(paste(getDataFromMatrix(sample2,c("sample.name"))[1],"input",sep="-"),length(input.files1)))
	print("sample.types")
	print(sample.types)

	bam.files <- c(medips.files1,input.files1,medips.files2,input.files2)

	#bam.files <- c(getDataFromMatrix(sample1,c("medips","input")),getDataFromMatrix(sample2,c("medips","input")))
	print("STATUS:Loading BAM files:")
	print(bam.files)
	bam.names <- c(getDataFromMatrix(sample1,c("replicate.name")),getDataFromMatrix(sample2,c("replicate.name")))
	print("bam.names")
	print(bam.names)
    granges <- lapply(bam.files,BAM2GRanges)
    grl <- mergeReplicates(GRangesList(granges),sample.types)
    names(grl) <- unique(sample.types) # With unique we remove the keep only one name per replicate
	sample.names <- c(getDataFromMatrix(sample1,c("sample.name"))[1],getDataFromMatrix(sample2,c("sample.name"))[1])
	print("sample.names")
	print(sample.names)
  }
    print("STATUS:Building feature scores:")
	print(paste("Building featureScores for",names(grl),"up =",up,"down =",down,"freq =",freq,"s.width =",s.width))

fs <- featureScores(grl,annot,up=up,down=down,freq=freq,s.width=s.width,verbose=TRUE,dist=dist)


  if (is.null(design)){
    print("TODO! Remove this. Right now it does the default behaviour (substract 1-2 and 3-4)")
    l.forScores <- list(tables(fs)[[1]] - tables(fs)[[2]], tables(fs)[[3]] - tables(fs)[[4]])
    fs@scores <- l.forScores
	names(fs) <- sample.names 
  }else{
    print("TODO! Consider different designs!")
  }
  fs
}


heatmapCluster <- function(featureScores=NULL,grl.stored=NULL,design=NULL,ma.file=NULL,expr.threshold=NULL,n.clusters=c(25),dirOut,expName,transcripts=NULL){
  
  if (!is.null(grl.stored)){
    if (!is.null(transcripts)){
      
      annot <- buildAnnotation(transcripts=transcripts)
    }else{
      stop("TODO! Load the annotation file from the file system. Precompute it!")
    }
    fs = buildFeatureScores(grl.stored=grl.stored,annot=annot)
  }
  else{
    fs = featureScores
  }

	if(is.null(expr.threshold)){
		expr.threshold <- c(0)
	}
	
	for(th in expr.threshold){
		for(cl in n.clusters){
  			if(!is.null(ma.file)){
				ma <- read.table(ma.file, header=T,row.names=1)
				expr <- ma[annot@elementMetadata$tx_name,1]
			    	expr[which((expr >= -th  & expr <= th))] <- NA
			}else{
			    	expr=NULL
			}

  
		  	if (is.null(design)){
				#l.forScores <- list(tables(featureScores)[[1]] - tables(featureScores)[[2]], tables(featureScores)[[3]] - tables(featureScores)[[4]])
				#featureScores@scores <- l.forScores
				file.name <- paste(expName,th,cl,sep="-")
				print(paste("Creating",file.path(dirOut,paste(file.name,".jpg",sep=""))))
				jpeg(file.path(dirOut,paste(file.name,".jpg",sep="")),quality=100,h=2400,w=800)
				cp=clusterPlots(fs,n.clusters=cl,expr=expr,plot.type='heatmap',t.name=file.name)
				dev.off()
				cl <- as.data.frame(getClusters(cp))
			}
			cl
		}
	}
  
}
# For getting a matrx (is it?) from the ClusterPlot cp
getClusters <- function(cp){
  cl <- clusters(cp)
  ids <- values(cp@anno)$gene_id
  an<-cbind(c,ids)
  #print(head(an))
  an
}


# From functions.R of the NAR paper
unfactor=function(var){
  if (is.factor(var)){
    tmp=names(var)
    tmpopt=getOption("warn")
    options(warn=-1)
    out=as.numeric(levels(var))[as.integer(var)]
    options(warn=tmpopt)
    if(any(is.na(out)) & any(is.na(out)!=is.na(var))){
      out=as.character(levels(var))[as.integer(var)]
    }
    names(out)=tmp
  }else if(is.data.frame(var)){
                                        #Have to use a loop, since calling apply will return a matrix
    out=var
    for(i in 1:dim(var)[2]){
      out[,i]=unfactor(var[,i])
    }
  }else if(is.list(var)){
                                        #Mmmmm, recursion
    out=lapply(var,unfactor)
  }else{
                                        #The default option
    out=var
  }
  return(out)
}
